package scopt

private[scopt] object platform {
  val _NL = System.getProperty("line.separator")

  import java.util.{Locale, Calendar}
  import java.io.File

  case class ParseException(message: String, errorOffset: Int) extends Exception(message)
  def mkParseEx(s: String, p: Int) = new ParseException(s, p)

  trait PlatformReadInstances {
    implicit val fileRead: Read[File] = Read.reads { new File(_) }
  }

  def applyArgumentExHandler[C](desc: String, arg: String): PartialFunction[Throwable, Either[Seq[String], C]] = {
      case e: NumberFormatException => Left(Seq(desc + " expects a number but was given '" + arg + "'"))
      case e: ParseException        => Left(Seq(desc + " expects a Scala duration but was given '" + arg + "'"))
      case e: Throwable             => Left(Seq(desc + " failed when given '" + arg + "'. " + e.getMessage))
    }
}
