import org.specs2._
import java.util.{Calendar, GregorianCalendar}
import java.io.{ByteArrayOutputStream, File}
import java.net.{ URI, InetAddress }
import scala.concurrent.duration.Duration

class HoldOutputSpec extends Specification { def is = args(sequential = true) ^ s2"""
  This is a specification to check the immutable parser

  opt[Int](('f', "foo") action { x => x } should
    fail to parse --foo bar                                     ${intParserHoldOutput("--foo", "bar", "--out", "put.me")}
    fail to parse missing required --out option                 ${intParserHoldOutput2("--foo", "bar")}

  help("help") if holding output should
    print usage text --help                                     ${helpParserHoldOutput("--help")}
     
                                                                """

  import SpecUtil._

  case class Config(foo: Int = -1, out: File = new File("."), xyz: Boolean = false,
    libName: String = "", maxCount: Int = -1, verbose: Boolean = false, debug: Boolean = false,
    mode: String = "", files: Seq[File] = Seq(), keepalive: Boolean = false,
    jars: Seq[File] = Seq(), kwargs: Map[String,String] = Map())

  val parser = new scopt.OptionParserHoldOutput[Config]("scopt") {
    override def renderingMode = scopt.RenderingMode.OneColumn
    head("scopt", "3.x")

    opt[Int]('f', "foo").action( (x, c) =>
      c.copy(foo = x) ).text("foo is an integer property")

    opt[File]('o', "out").required().valueName("<file>").
      action( (x, c) => c.copy(out = x) ).
      text("out is a required file property")

    opt[(String, Int)]("max").action({
        case ((k, v), c) => c.copy(libName = k, maxCount = v) }).
      validate( x =>
        if (x._2 > 0) success
        else failure("Value <max> must be >0") ).
      keyValueName("<libname>", "<max>").
      text("maximum count for <libname>")

    opt[Seq[File]]('j', "jars").valueName("<jar1>,<jar2>...").action( (x,c) =>
      c.copy(jars = x) ).text("jars to include")

    opt[Map[String,String]]("kwargs").valueName("k1=v1,k2=v2...").action( (x, c) =>
      c.copy(kwargs = x) ).text("other arguments")

    opt[Unit]("verbose").action( (_, c) =>
      c.copy(verbose = true) ).text("verbose is a flag")

    opt[Unit]("debug").hidden().action( (_, c) =>
      c.copy(debug = true) ).text("this option is hidden in the usage text")

    help("help").text("prints this usage text")

    arg[File]("<file>...").unbounded().optional().action( (x, c) =>
      c.copy(files = c.files :+ x) ).text("optional unbounded args")

    note("some notes.".newline)

    cmd("update").action( (_, c) => c.copy(mode = "update") ).
      text("update is a command.").
      children(
        opt[Unit]("not-keepalive").abbr("nk").action( (_, c) =>
          c.copy(keepalive = false) ).text("disable keepalive"),
        opt[Boolean]("xyz").action( (x, c) =>
          c.copy(xyz = x) ).text("xyz is a boolean property"),
        opt[Unit]("debug-update").hidden().action( (_, c) =>
          c.copy(debug = true) ).text("this option is hidden in the usage text"),
        checkConfig( c =>
          if (c.keepalive && c.xyz) failure("xyz cannot keep alive")
          else success )
    )
  }

  def helpParserHoldOutput(args: String*) = {
    parser.parse(args.toSeq, Config())
    val expectedUsage = """scopt 3.x
Usage: scopt [update] [options] [<file>...]

  -f <value> | --foo <value>
        foo is an integer property
  -o <file> | --out <file>
        out is a required file property
  --max:<libname>=<max>
        maximum count for <libname>
  -j <jar1>,<jar2>... | --jars <jar1>,<jar2>...
        jars to include
  --kwargs k1=v1,k2=v2...
        other arguments
  --verbose
        verbose is a flag
  --help
        prints this usage text
  <file>...
        optional unbounded args
some notes.

Command: update [options]
update is a command.
  -nk | --not-keepalive
        disable keepalive
  --xyz <value>
        xyz is a boolean property""".newlines
    val expectedHeader = """scopt 3.x"""

    (parser.getOut === expectedUsage+"\n")  }

  def intParserHoldOutput(args: String*) = {
    val result = parser.parse(args.toSeq, Config())
    result === None && parser.getOut === "" && parser.getErr === "Error: Option --foo expects a number but was given 'bar'\nTry --help for more information.\n" 
  }

  def intParserHoldOutput2(args: String*) = {
    val result = parser.parse(args.toSeq, Config())
    result === None && parser.getOut === "" && parser.getErr === "Error: Option --foo expects a number but was given 'bar'\nError: Missing option --out\nTry --help for more information.\n" 
  }
 }
