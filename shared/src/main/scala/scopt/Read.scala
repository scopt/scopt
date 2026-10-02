package scopt

import java.net.URI
import scala.collection.{ Seq => CSeq }
import scala.collection.immutable.{ Seq => ISeq }

trait Read[A] { self =>
  def arity: Int
  def isVariadic: Boolean = false
  def tokensToRead: Int = if (arity == 0) 0 else 1
  def reads: String => A

  /** Reads multiple tokens into a single value (as in `--foo 1 2 3`).
   *
   * The default implementation joins the tokens with `Read.sep` and delegates to [[reads]].
   */
  def readsMany(tokens: CSeq[String]): A = reads(tokens.mkString(Read.sep))

  def map[B](f: A => B): Read[B] = new Read[B] {
    val arity = self.arity
    override val isVariadic = self.isVariadic
    override def readsMany(tokens: CSeq[String]): B = f(self.readsMany(tokens))
    val reads = self.reads andThen f
  }
}

/** A space-separated list of values, as in `--foo 1 2 3`.
 *
 * Unlike `Seq`, which remains comma-separated (`--foo 1,2,3`), this newtype
 * opts into consuming consecutive tokens until the next option, command, or `--`.
 */
final case class SpaceSep[A](list: List[A])

object Read extends platform.PlatformReadInstances {

  import scala.concurrent.duration.{ Duration, FiniteDuration }

  def reads[A](f: String => A): Read[A] = new Read[A] {
    val arity = 1
    val reads = f
  }
  implicit val stringRead: Read[String] = reads { identity }
  implicit val charRead: Read[Char] =
    reads {
      _.toCharArray match {
        case Array(char) => char
        case s           =>
          throw new IllegalArgumentException("'" + s + "' is not a char.")
      }
    }
  implicit val doubleRead: Read[Double] = reads { _.toDouble }
  implicit val booleanRead: Read[Boolean] =
    reads {
      _.toLowerCase match {
        case "true"  => true
        case "false" => false
        case "yes"   => true
        case "no"    => false
        case "1"     => true
        case "0"     => false
        case s       =>
          throw new IllegalArgumentException("'" + s + "' is not a boolean.")
      }
    }

  private def fixedPointWithRadix(str: String): (String, Int) = str.toLowerCase match {
    case s if s.startsWith("0x") => (s.stripPrefix("0x"), 16)
    case s                       => (s, 10)
  }
  implicit val intRead: Read[Int] = reads { str =>
    val (s, radix) = fixedPointWithRadix(str)
    Integer.parseInt(s, radix)
  }
  implicit val longRead: Read[Long] = reads { str =>
    val (s, radix) = fixedPointWithRadix(str)
    java.lang.Long.parseLong(s, radix)
  }
  implicit val shortRead: Read[Short] = reads { str =>
    val (s, radix) = fixedPointWithRadix(str)
    java.lang.Short.parseShort(s, radix)
  }
  implicit val bigIntRead: Read[BigInt] = reads { str =>
    val (s, radix) = fixedPointWithRadix(str)
    BigInt(s, radix)
  }

  implicit val bigDecimalRead: Read[BigDecimal] = reads { BigDecimal(_) }

  implicit val durationRead: Read[Duration] =
    reads {
      try {
        Duration(_)
      } catch {
        case e: NumberFormatException => throw platform.mkParseEx(e.getMessage, -1)
      }
    }

  implicit val finiteDurationRead: Read[FiniteDuration] =
    durationRead.map {
      case d: FiniteDuration => d
      case d => throw new IllegalArgumentException("'" + d + "' is not a finite duration.")
    }

  implicit def tupleRead[A1: Read, A2: Read]: Read[(A1, A2)] = new Read[(A1, A2)] {
    val arity = 2
    val reads = { (s: String) =>
      splitKeyValue(s) match {
        case (k, v) => implicitly[Read[A1]].reads(k) -> implicitly[Read[A2]].reads(v)
      }
    }
  }
  private def splitKeyValue(s: String): (String, String) =
    s.indexOf('=') match {
      case -1     => throw new IllegalArgumentException("Expected a key=value pair")
      case n: Int => (s.slice(0, n), s.slice(n + 1, s.length))
    }
  implicit val unitRead: Read[Unit] = new Read[Unit] {
    val arity = 0
    val reads = { (s: String) =>
      ()
    }
  }

  val sep = ","

  // reads("1,2,3,4,5") == Seq(1,2,3,4,5)
  implicit def seqRead[A: Read]: Read[CSeq[A]] = reads { (s: String) =>
    s.split(sep).toList.map(implicitly[Read[A]].reads)
  }
  // reads("1,2,3,4,5") == List(1,2,3,4,5)
  implicit def immutableSeqRead[A: Read]: Read[ISeq[A]] = reads { (s: String) =>
    s.split(sep).toList.map(implicitly[Read[A]].reads)
  }

  // readsMany(Seq("1", "2", "3")) == SpaceSep(List(1, 2, 3))
  implicit def spaceSepRead[A: Read]: Read[SpaceSep[A]] = new Read[SpaceSep[A]] {
    override val arity: Int = 1
    override val isVariadic: Boolean = true
    override val reads: String => SpaceSep[A] =
      (s: String) => SpaceSep(List(implicitly[Read[A]].reads(s)))
    override def readsMany(tokens: CSeq[String]): SpaceSep[A] =
      SpaceSep(tokens.iterator.map(implicitly[Read[A]].reads).toList)
  }

  // reads("1=false,2=true") == Map(1 -> false, 2 -> true)
  implicit def mapRead[K: Read, V: Read]: Read[Map[K, V]] = reads { (s: String) =>
    s.split(sep).map(implicitly[Read[(K, V)]].reads).toMap
  }

  // reads("1=false,1=true") == List((1 -> false), (1 -> true))
  implicit def seqTupleRead[K: Read, V: Read]: Read[CSeq[(K, V)]] = reads { (s: String) =>
    s.split(sep).map(implicitly[Read[(K, V)]].reads).toList
  }
  // reads("1=false,1=true") == List((1 -> false), (1 -> true))
  implicit def immutableSeqTupleRead[K: Read, V: Read]: Read[ISeq[(K, V)]] = reads { (s: String) =>
    s.split(sep).map(implicitly[Read[(K, V)]].reads).toList
  }

  implicit def optionRead[A: Read]: Read[Option[A]] = reads {
    case ""  => None
    case str => Some(implicitly[Read[A]].reads(str))
  }

  implicit val uriRead: Read[URI] = Read.reads { new URI(_) }
}
