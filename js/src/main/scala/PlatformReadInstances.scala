package scopt

import scala.scalajs.js
import scala.scalajs.js.annotation._
import collection.{ Seq => CSeq }

// js version

@js.native
@JSImport("os", JSImport.Namespace)
object OS extends js.Object {
  val EOL: String = js.native
}

class ParseException(s: String, errorOffset: Int) extends Exception(s)

private[scopt] object platform {
  val _NL = System.lineSeparator()

  type ParseException = scopt.ParseException
  def mkParseEx(s: String, p: Int) = new ParseException(s, p)

  trait PlatformReadInstances {}

  def applyArgumentExHandler[C](
      desc: String,
      arg: String
  ): PartialFunction[Throwable, Either[CSeq[String], C]] = {
    case e: NumberFormatException =>
      Left(List(desc + " expects a number but was given '" + arg + "'"))
    case e: ParseException =>
      Left(List(desc + " expects a Scala duration but was given '" + arg + "'"))
    case e: Throwable => Left(List(desc + " failed when given '" + arg + "'. " + e.getMessage))
  }

  def exit(status: Int): Unit = ()
}
