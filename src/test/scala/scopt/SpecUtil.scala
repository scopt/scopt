
object SpecUtil {
  val envLineSeparator = util.Properties.lineSeparator
  implicit class RichString(self: String) {
    def newline: String = self + envLineSeparator
    def newlines: String = self.lines.mkString(envLineSeparator)
  }
}
