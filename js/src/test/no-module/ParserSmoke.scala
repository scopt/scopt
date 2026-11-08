import scala.scalajs.js.annotation.JSExportTopLevel
import scopt.OParser

object ParserSmoke {
  @JSExportTopLevel("checkParser")
  def checkParser(): Unit = {
    val builder = OParser.builder[String]
    import builder._
    val parser = OParser.sequence(
      programName("example"),
      opt[String]("name").action((value, _) => value).text("Display name")
    )
    assert(OParser.parse(parser, Seq("--name", "Ada"), "").contains("Ada"))

    val help = OParser.usage(parser)
    assert(help.startsWith("Usage: example"))
    assert(help.contains("\n") && !help.contains("\r"))
    assert(help.contains("--name") && help.contains("Display name"))
  }
}
