package scopttest

import SpecUtilPlatform._
import java.io.ByteArrayOutputStream

object SpecUtil {
  implicit class RichString(self: String) {
    def newline: String = self + envLineSeparator
    def newlines: String =
      self.linesIterator.mkString(envLineSeparator)

    def linesList: List[String] =
      self.linesIterator.toList
  }

  def printParserOut(thunk: => Unit): String = {
    val outStream = new ByteArrayOutputStream()
    Console.withOut(outStream) { thunk }
    outStream.toString("UTF-8")
  }

  def printParserError(thunk: => Unit): String = {
    val errStream = new ByteArrayOutputStream()
    Console.withErr(errStream) { thunk }
    errStream.toString("UTF-8")
  }
}
