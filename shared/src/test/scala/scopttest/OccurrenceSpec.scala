package scopttest

import scala.collection.mutable.ListBuffer
import scopt._
import scopt.OEffect._

object OccurrenceSpec extends verify.BasicTestSuite {
  private val setup = new DefaultOParserSetup {
    override def showUsageOnError: Option[Boolean] = Some(false)
  }
  private def limit(name: String, count: Int): String =
    s"Option --$name may only be given $count ${if (count == 1) "time" else "times"}"

  private def flag(max: Int = 1): OParser[Unit, Int] = {
    val option = OParser.builder[Int].opt[Unit]('v', "verbose")
    val bounded = if (max == Int.MaxValue) option.unbounded() else option.maxOccurs(max)
    bounded.action((_, count) => count + 1)
  }

  test("object parser reports exhausted aliases without applying them again") {
    val cases = List(
      List("--verbose", "--verbose"),
      List("--verbose", "-v"),
      List("-v", "--verbose"),
      List("-v", "-v"),
      List("-vv"),
      List("-vvv")
    )
    for (args <- cases) {
      var applied = 0
      val effects = ListBuffer[OEffect]()
      val parser = new OptionParser[Int]("test") {
        opt[Unit]('v', "verbose").action { (_, count) => applied += 1; count + 1 }
        override def showUsageOnError: Option[Boolean] = Some(false)
        override def reportError(msg: String): Unit = { effects += ReportError(msg); () }
      }
      assert(parser.parse(args, 0).isEmpty)
      assert(applied == 1)
      assert(effects.toList == List(ReportError(limit("verbose", 1))))
    }
  }

  test("monadic parser retains bounded and unbounded occurrence counts") {
    assert(OParser.runParser(flag(2), List("-vv"), 0, setup) == (Some(2), Nil))
    assert(
      OParser.runParser(flag(2), List("-vvv"), 0, setup) ==
        (None, List(ReportError(limit("verbose", 2))))
    )
    assert(OParser.runParser(flag(Int.MaxValue), List("-vvvv"), 0, setup) == (Some(4), Nil))
  }

  test("exhausted value options retain their original value consumption") {
    val cases = List(
      (List("--number", "1", "--number", "2"), List("2")),
      (List("-n", "1", "-n", "2"), List("2")),
      (List("--number=1", "--number:2"), Nil),
      (List("-n=1", "-n:2"), Nil)
    )
    for ((args, remaining) <- cases) {
      val builder = OParser.builder[Int]
      var numbers = List.empty[Int]
      var words = List.empty[String]
      val parser = OParser.sequence(
        builder.opt[Int]('n', "number").action { (n, count) => numbers :+= n; count + 1 },
        builder.arg[String]("rest").optional().unbounded().action { (s, count) =>
          words :+= s; count
        }
      )
      assert(
        OParser.runParser(parser, args, 0, setup) ==
          (None, List(ReportError(limit("number", 1))))
      )
      assert(numbers == List(1))
      assert(words == remaining)
    }
  }

  test("grouped bare value options remain unknown while standalone repeats report their limit") {
    def check(
        args: List[String],
        errors: List[String],
        expectedNumbers: List[Int],
        expectedFlags: Int,
        expectedWords: List[String],
        result: Option[Int] = None
    ): Unit = {
      val builder = OParser.builder[Int]
      var numbers = List.empty[Int]
      var flags = 0
      var words = List.empty[String]
      val parser = OParser.sequence(
        builder.opt[Unit]('a', "all").action { (_, count) => flags += 1; count + 1 },
        builder.opt[Int]('n', "number").action { (n, count) => numbers :+= n; count + 1 },
        builder.arg[String]("rest").optional().unbounded().action { (s, count) =>
          words :+= s; count
        }
      )
      assert(
        OParser.runParser(parser, args, 0, setup) ==
          (result, errors.map(ReportError.apply))
      )
      assert(numbers == expectedNumbers)
      assert(flags == expectedFlags)
      assert(words == expectedWords)
    }
    check(List("-n", "1", "-an"), List("Unknown option -n"), List(1), 1, Nil)
    check(List("-n", "1", "-n", "2"), List(limit("number", 1)), List(1), 0, List("2"))
    check(List("-n", "1", "-an=2"), List(limit("number", 1)), List(1), 1, Nil)
    check(List("-n", "1", "-an:2"), List(limit("number", 1)), List(1), 1, Nil)
    check(List("-an", "2"), List("Unknown option -n"), Nil, 1, List("2"))
    check(List("-an=2"), List("Option --number expects a number but was given ''"), Nil, 1, Nil)
  }

  test("unknown options and inactive sibling options remain unknown") {
    val builder = OParser.builder[Int]
    val parser = OParser.sequence(
      builder.cmd("one").children(builder.opt[Unit]('v', "verbose")),
      builder.cmd("two").children(builder.opt[Unit]('x', "extra"))
    )
    for (name <- List("--verbose", "-v", "--unknown")) {
      assert(
        OParser.runParser(parser, List("two", name), 0, setup) ==
          (None, List(ReportError("Unknown option " + name)))
      )
    }
    assert(
      OParser.runParser(flag(), List("-x"), 0, setup) ==
        (None, List(ReportError("Unknown option -x")))
    )
    val number = builder.opt[Int]('n', "number").action((_, count) => count + 1)
    for (name <- List("-n2", "-number", "--numberish")) {
      assert(
        OParser.runParser(number, List("--number", "1", name), 0, setup) ==
          (None, List(ReportError("Unknown option " + name)))
      )
    }
  }

  test("end of options and positional diagnostics retain their behavior") {
    val builder = OParser.builder[Int]
    var words = List.empty[String]
    val parser = OParser.sequence(
      flag(),
      builder.arg[String]("rest").optional().unbounded().action { (s, count) =>
        words :+= s; count
      }
    )
    assert(OParser.runParser(parser, List("-v", "--", "-v"), 0, setup) == (Some(1), Nil))
    assert(words == List("-v"))
    assert(
      OParser.runParser(flag(), List("-v", "--", "-v"), 0, setup) ==
        (None, List(ReportError("Unknown argument '-v'")))
    )
    assert(
      OParser.runParser(flag(), List("word"), 0, setup) ==
        (None, List(ReportError("Unknown argument 'word'")))
    )
  }

  test("active grouped flags take precedence over an exhausted longer alias") {
    val builder = OParser.builder[Int]
    val parser = OParser.sequence(
      builder.opt[Unit]("both").abbr("ab").action((_, count) => count + 1),
      builder.opt[Unit]('a', "first").action((_, count) => count + 1),
      builder.opt[Unit]('b', "second").action((_, count) => count + 1)
    )
    assert(OParser.runParser(parser, List("-ab", "-ab"), 0, setup) == (Some(3), Nil))
    assert(
      OParser.runParser(parser, List("-ab", "-ab", "-ab"), 0, setup) ==
        (None, List(ReportError(limit("both", 1))))
    )
  }

  test("warning mode retains successful config and structured effects") {
    val warningSetup = new DefaultOParserSetup {
      override def errorOnUnknownArgument: Boolean = false
    }
    assert(
      OParser.runParser(flag(), List("-vv", "-x"), 0, warningSetup) ==
        (Some(1), List(ReportWarning(limit("verbose", 1)), ReportWarning("Unknown option -x")))
    )
  }

  test("exhausted options retain the selected command's occurrence limit") {
    val builder = OParser.builder[Int]
    val parser = OParser.sequence(
      builder.cmd("one").children(flag()),
      builder.cmd("two").children(flag(2))
    )
    assert(OParser.runParser(parser, List("two", "-vv"), 0, setup) == (Some(2), Nil))
    assert(
      OParser.runParser(parser, List("two", "-vvv"), 0, setup) ==
        (None, List(ReportError(limit("verbose", 2))))
    )
  }
}
