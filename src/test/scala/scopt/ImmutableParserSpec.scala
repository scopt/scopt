import org.specs2._
import java.util.{Calendar, GregorianCalendar}
import java.io.{ByteArrayOutputStream, File}
import java.net.URI

class ImmutableParserSpec extends Specification { def is =      s2"""
  This is a specification to check the immutable parser
  
  opt[Unit]('f', "foo") action { x => x } should
    parse () out of --foo                                       ${unitParser("--foo")}
    parse () out of -f                                          ${unitParser("-f")} 

  opt[Unit]('f', "foo") action { x => x }; opt[Unit]('b', "bar") should
    parse () out of -fb                                         ${groupParser("-fb")}

  opt[Int]('f', "foo") action { x => x } should
    parse 1 out of --foo 1                                      ${intParser("--foo", "1")}
    parse 1 out of --foo:1                                      ${intParser("--foo:1")}
    parse 1 out of -f 1                                         ${intParser("-f", "1")}
    parse 1 out of -f:1                                         ${intParser("-f:1")}
    fail to parse --foo                                         ${intParserFail{"--foo"}}
    fail to parse --foo bar                                     ${intParserFail("--foo", "bar")}

  opt[String]("foo") action { x => x } should
    parse "bar" out of --foo bar                                ${stringParser("--foo", "bar")}
    parse "bar" out of --foo:bar                                ${stringParser("--foo:bar")}

  opt[Double]("foo") action { x => x } should
    parse 1.0 out of --foo 1.0                                  ${doubleParser("--foo", "1.0")}
    parse 1.0 out of --foo:1.0                                  ${doubleParser("--foo:1.0")}
    fail to parse --foo bar                                     ${doubleParserFail("--foo", "bar")}

  opt[Boolean]("foo") action { x => x } should
    parse true out of --foo true                                ${trueParser("--foo", "true")}
    parse true out of --foo:true                                ${trueParser("--foo:true")}
    parse true out of --foo 1                                   ${trueParser("--foo", "1")}
    parse true out of --foo:1                                   ${trueParser("--foo:1")}
    fail to parse --foo bar                                     ${boolParserFail("--foo", "bar")}

  opt[BigDecimal]("foo") action { x => x } should
    parse 1.0 out of --foo 1.0                                  ${bigDecimalParser("--foo", "1.0")}
    fail to parse --foo bar                                     ${bigDecimalParserFail("--foo", "bar")}

  opt[Calendar]("foo") action { x => x } should
    parse 2000-01-01 out of --foo 2000-01-01                    ${calendarParser("--foo", "2000-01-01")}
    fail to parse --foo bar                                     ${calendarParserFail("--foo", "bar")}

  opt[File]("foo") action { x => x } should
    parse test.txt out of --foo test.txt                        ${fileParser("--foo", "test.txt")}

  opt[URI]("foo") action { x => x } should
    parse http://github.com/ out of --foo http://github.com/    ${uriParser("--foo", "http://github.com/")}

  opt[(String, Int)]("foo") action { x => x } should
    parse ("k", 1) out of --foo k=1                             ${pairParser("--foo", "k=1")}
    parse ("k", 1) out of --foo:k=1                             ${pairParser("--foo:k=1")}
    fail to parse --foo                                         ${pairParserFail("--foo")}
    fail to parse --foo bar                                     ${pairParserFail("--foo", "bar")}
    fail to parse --foo k=bar                                   ${pairParserFail("--foo", "k=bar")}

  opt[String]("foo") required() action { x => x } should
    fail to parse Nil                                           ${requiredFail()}

  unknown options should
    fail to parse by default                                    ${intParserFail("-z", "bar")}

  opt[(String, Int)]("foo") action { x => x } validate { x =>
    if (x > 0) success else failure("Option --foo must be >0") } should
    fail to parse --foo 0                                       ${validFail("--foo", "0")}

  arg[Int]("<port>") action { x => x } should
    parse 80 out of 80                                          ${intArg("80")}
    be required and should fail to parse Nil                    ${intArgFail()}

  arg[String]("<a>"); arg[String]("<b>") action { x => x } should
    parse "b" out of a b                                        ${multipleArgs("a", "b")}

  arg[String]("<a>") action { x => x} unbounded() optional(); arg[String]("<b>") optional() should
    parse "b" out of a b                                        ${unboundedArgs("a", "b")}
    parse nothing out of Nil                                    ${emptyArgs()}

  cmd("update") action { x => x } children { opt[Unit]("foo") action { x => x} } should
    parse () out of update                                      ${cmdParser("update")}
    parse () out of update --foo                                ${cmdParser("update", "--foo")}
    fail to parse --foo                                         ${cmdParserFail("--foo")}

   arg[String]("<a>") action { x => x}; cmd("update") children { arg[String]("<b>") } ; cmd("commit") should
    parse commit out of update foo commit                       ${cmdPosParser("update", "foo", "commit")}
    fail to parse foo update                                    ${cmdPosParserFail("foo", "update")}

   cmd("backend") children { cmd("update") children { arg[String]("<a>") action { x => x} } } should
    parse foo out of backend update foo                         ${nestedCmdParser("backend", "update", "foo")}
    fail to paser backend foo                                   ${nestedCmdParserFail("backend", "foo")}

  help("help") should
    print usage text --help                                     ${helpParser()}

  reportError("foo") should
    print "Error: foo\n"                                        ${reportErrorParser("foo")}

  reportWarning("foo") should
    print "Warning: foo\n"                                      ${reportWarningParser("foo")}

  showHeader should
    print "scopt 3.x\n"                                         ${showHeaderParser()}

  showUsage should
    print usage text                                            ${showUsageParser()}
                                                                """

  val unitParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Unit]('f', "foo") action { (x, c) => c.copy(flag = true) }
  }
  def unitParser(args: String*) = {
    val result = unitParser1.parse(args.toSeq, Config())
    result.get.flag === true
  }

  val groupParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Unit]('f', "foo") action { (x, c) => c.copy(flag = true) }
    opt[Unit]('b', "bar")
  }
  def groupParser(args: String*) = {
    val result = groupParser1.parse(args.toSeq, Config())
    result.get.flag === true
  }

  val intParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Int]('f', "foo") action { (x, c) => c.copy(intValue = x) }
  }
  def intParser(args: String*) = {
    val result = intParser1.parse(args.toSeq, Config())
    result.get.intValue === 1
  }
  def intParserFail(args: String*) = {
    val result = intParser1.parse(args.toSeq, Config())
    result === None
  }

  val stringParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[String]("foo") action { (x, c) => c.copy(stringValue = x) }
  }
  def stringParser(args: String*) = {
    val result = stringParser1.parse(args.toSeq, Config())
    result.get.stringValue === "bar"
  }

  val doubleParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Double]("foo") action { (x, c) => c.copy(doubleValue = x) }
  }
  def doubleParser(args: String*) = {
    val result = doubleParser1.parse(args.toSeq, Config())
    result.get.doubleValue === 1.0
  }
  def doubleParserFail(args: String*) = {
    val result = doubleParser1.parse(args.toSeq, Config())
    result === None
  }

  val boolParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Boolean]("foo") action { (x, c) => c.copy(boolValue = x) }
  }
  def trueParser(args: String*) = {
    val result = boolParser1.parse(args.toSeq, Config())
    result.get.boolValue === true
  }
  def boolParserFail(args: String*) = {
    val result = boolParser1.parse(args.toSeq, Config())
    result === None
  }

  val bigDecimalParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[BigDecimal]("foo") action { (x, c) => c.copy(bigDecimalValue = x) }
  }
  def bigDecimalParser(args: String*) = {
    val result = bigDecimalParser1.parse(args.toSeq, Config())
    result.get.bigDecimalValue === BigDecimal("1.0")
  }
  def bigDecimalParserFail(args: String*) = {
    val result = bigDecimalParser1.parse(args.toSeq, Config())
    result === None
  }

  val calendarParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Calendar]("foo") action { (x, c) => c.copy(calendarValue = x) }
  }
  def calendarParser(args: String*) = {
    val result = calendarParser1.parse(args.toSeq, Config())
    result.get.calendarValue.getTime === new GregorianCalendar(2000, Calendar.JANUARY, 1).getTime
  }
  def calendarParserFail(args: String*) = {
    val result = calendarParser1.parse(args.toSeq, Config())
    result === None
  }

  val fileParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[File]("foo") action { (x, c) => c.copy(fileValue = x) }
  }
  def fileParser(args: String*) = {
    val result = fileParser1.parse(args.toSeq, Config())
    result.get.fileValue === new File("test.txt")
  }
  
  val uriParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[URI]("foo") action { (x, c) => c.copy(uriValue = x) }
  }
  def uriParser(args: String*) = {
    val result = uriParser1.parse(args.toSeq, Config())
    result.get.uriValue === new URI("http://github.com/")
  }

  val pairParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[(String, Int)]("foo") action { case ((k, v), c) => c.copy(key = k, intValue = v) }
  }
  def pairParser(args: String*) = {
    val result = pairParser1.parse(args.toSeq, Config())
    (result.get.key === "k") and (result.get.intValue === 1)
  }
  def pairParserFail(args: String*) = {
    val result = pairParser1.parse(args.toSeq, Config())
    result === None
  }

  val requireParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[String]("foo") required() action { (x, c) => c.copy(stringValue = x) }
  }
  def requiredFail(args: String*) = {
    val result = requireParser1.parse(args.toSeq, Config())
    result === None
  }

  val validParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Int]('f', "foo") action { (x, c) => c.copy(intValue = x) } validate { x =>
      if (x > 0) success else failure("Option --foo must be >0") } validate { x =>
      failure("Just because") }
  }
  def validFail(args: String*) = {
    val result = validParser1.parse(args.toSeq, Config())
    result === None
  }

  val intArgParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    arg[Int]("<port>") action { (x, c) => c.copy(intValue = x) }
  }
  def intArg(args: String*) = {
    val result = intArgParser1.parse(args.toSeq, Config())
    result.get.intValue === 80
  }
  def intArgFail(args: String*) = {
    val result = intArgParser1.parse(args.toSeq, Config())
    result === None
  }

  val multipleArgsParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    arg[String]("<a>") action { (x, c) => c.copy(a = x) }
    arg[String]("<b>") action { (x, c) => c.copy(b = x) }
  }
  def multipleArgs(args: String*) = {
    val result = multipleArgsParser1.parse(args.toSeq, Config())
    (result.get.a === "a") and (result.get.b === "b")
  }

  val unboundedArgsParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    arg[String]("<a>") action { (x, c) => c.copy(a = x) } unbounded() optional()
    arg[String]("<b>") action { (x, c) => c.copy(b = x) } optional()
  }
  def unboundedArgs(args: String*) = {
    val result = unboundedArgsParser1.parse(args.toSeq, Config())
    (result.get.a === "b") and (result.get.b === "")
  }
  def emptyArgs(args: String*) = {
    val result = unboundedArgsParser1.parse(args.toSeq, Config())
    (result.get.a === "") and (result.get.b === "")
  }

  val cmdParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    cmd("update") action { (x, c) => c.copy(flag = true) } children {
      opt[Unit]("foo") action { (x, c) => c.copy(stringValue = "foo") }
    }
  }
  def cmdParser(args: String*) = {
    val result = cmdParser1.parse(args.toSeq, Config())
    result.get.flag === true
  }
  def cmdParserFail(args: String*) = {
    val result = cmdParser1.parse(args.toSeq, Config())
    result === None
  }

  val cmdPosParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    arg[String]("<a>") action { (x, c) => c.copy(a = x) } 
    cmd("update") action { (x, c) => c.copy(flag = true) } children {
      arg[String]("<b>") action { (x, c) => c.copy(b = x) } 
    }
    cmd("commit")
  }
  def cmdPosParser(args: String*) = {
    val result = cmdPosParser1.parse(args.toSeq, Config())
    result.get.a === "commit"
  }
  def cmdPosParserFail(args: String*) = {
    val result = cmdPosParser1.parse(args.toSeq, Config())
    result === None    
  }

  val nestedCmdParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    cmd("backend") text("commands to manipulate backends:\n") action { (x, c) =>
      c.copy(flag = true) } children {
      cmd("update") children {
        arg[String]("<a>") action { (x, c) => c.copy(a = x) } 
      }     
    }
  }
  def nestedCmdParser(args: String*) = {
    val result = nestedCmdParser1.parse(args.toSeq, Config())
    result.get.a === "foo"
  }
  def nestedCmdParserFail(args: String*) = {
    val result = nestedCmdParser1.parse(args.toSeq, Config())
    result === None    
  }

  def helpParser(args: String*) = {
    case class Config(foo: Int = -1, out: File = new File("."), xyz: Boolean = false,
      libName: String = "", maxCount: Int = -1, verbose: Boolean = false,
      mode: String = "", files: Seq[File] = Seq())
    val parser = new scopt.OptionParser[Config]("scopt") {
      head("scopt", "3.x")
      opt[Int]('f', "foo") action { (x, c) =>
        c.copy(foo = x) } text("foo is an integer property")
      opt[File]('o', "out") required() valueName("<file>") action { (x, c) =>
        c.copy(out = x) } text("out is a required file property")
      opt[(String, Int)]("max") action { case ((k, v), c) =>
        c.copy(libName = k, maxCount = v) } validate { x =>
        if (x._2 > 0) success else failure("Value <max> must be >0") 
      } keyValueName("<libname>", "<max>") text("maximum count for <libname>")
      opt[Unit]("verbose") action { (_, c) =>
        c.copy(verbose = true) } text("verbose is a flag")
      help("help") text("prints this usage text")
      arg[File]("<file>...") unbounded() optional() action { (x, c) =>
        c.copy(files = c.files :+ x) } text("optional unbounded args")
      note("some notes.\n")
      cmd("update") action { (_, c) =>
        c.copy(mode = "update") } text("update is a command.") children {
        opt[Boolean]("xyz") action { (x, c) =>
          c.copy(xyz = x) } text("xyz is a boolean property")
      }
    }
    parser.parse(args.toSeq, Config())
    val expectedUsage = """scopt 3.x
Usage: scopt [update] [options] [<file>...]

  -f <value> | --foo <value>
        foo is an integer property
  -o <file> | --out <file>
        out is a required file property
  --max:<libname>=<max>
        maximum count for <libname>
  --verbose
        verbose is a flag
  --help
        prints this usage text
  <file>...
        optional unbounded args
some notes.

Command: update [options]
update is a command.
  --xyz <value>
        xyz is a boolean property"""
    val expectedHeader = """scopt 3.x"""

    (parser.header === expectedHeader) and (parser.usage === expectedUsage)
  }

  val printParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    help("help") text("prints this usage text")
  }
  def printParser(body: scopt.OptionParser[Config] => Unit): String = {
    val bos = new ByteArrayOutputStream()
    Console.withErr(bos) { body(printParser1) }
    bos.toString("UTF-8")
  }
  def reportErrorParser(msg: String) = {
    printParser(_.reportError(msg)) === "Error: foo\n"
  }
  def reportWarningParser(msg: String) = {
    printParser(_.reportWarning(msg)) === "Warning: foo\n"
  }
  def showHeaderParser() = {
    printParser(_.showHeader) === "scopt 3.x\n"
  }
  def showUsageParser() = {
    printParser(_.showUsage) === """scopt 3.x
Usage: scopt [options]

  --help
        prints this usage text
"""
  }

  case class Config(flag: Boolean = false, intValue: Int = 0, stringValue: String = "",
    doubleValue: Double = 0.0, boolValue: Boolean = false,
    bigDecimalValue: BigDecimal = BigDecimal("0.0"),
    calendarValue: Calendar = new GregorianCalendar(1900, Calendar.JANUARY, 1),
    fileValue: File = new File("."),
    uriValue: URI = new URI("http://localhost"),
    key: String = "", a: String = "", b: String = "")
}
