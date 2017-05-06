import org.specs2._
import java.util.{Calendar, GregorianCalendar}
import java.io.{ByteArrayOutputStream, File}
import java.net.{ URI, InetAddress }
import scala.concurrent.duration.Duration

class ImmutableParserSpec extends Specification { def is = args(sequential = true) ^ s2"""
  This is a specification to check the immutable parser

  opt[Unit]('f', "foo") action { x => x } should
    parse () out of --foo                                       ${unitParser("--foo")}
    parse () out of -f                                          ${unitParser("-f")}

  opt[Unit]('a', "alice"); opt[Unit]('b', "bob"); opt[Unit]("alicebob") abbr("ab") action { x => x } should
    parse () out of -ab                                         ${groupParser("-ab")}
    parse () out of -abab                                       ${groupParser("-abab")}

  opt[Int]('f', "foo") action { x => x } should
    parse 1 out of --foo 1                                      ${intParser("--foo", "1")}
    parse 1 out of --foo:1                                      ${intParser("--foo:1")}
    parse 1 out of --foo=1                                      ${intParser("--foo=1")}
    parse 1 out of -f 1                                         ${intParser("-f", "1")}
    parse 1 out of -f:1                                         ${intParser("-f:1")}
    parse 1 out of -f=1                                         ${intParser("-f=1")}
    parse 1 out of --foo 0x01                                   ${intParser("--foo", "0x01")}
    parse 1 out of --foo:0x01                                   ${intParser("--foo:0x01")}
    parse 1 out of --foo=0x01                                   ${intParser("--foo=0x01")}
    parse 1 out of -f 0x1                                       ${intParser("-f", "0x1")}
    parse 1 out of -f:0x1                                       ${intParser("-f:0x1")}
    fail to parse --foo                                         ${intParserFail{"--foo"}}
    fail to parse --foo bar                                     ${intParserFail("--foo", "bar")}
    fail to parse --foo=bar                                     ${intParserFail("--foo=bar")}

  opt[Long]('f', "foo") action { x => x } should
    parse 1 out of --foo 1                                      ${longParser("--foo", "1")}
    parse 1 out of --foo:1                                      ${longParser("--foo:1")}
    parse 1 out of --foo=1                                      ${longParser("--foo=1")}
    parse 1 out of -f 1                                         ${longParser("-f", "1")}
    parse 1 out of -f:1                                         ${longParser("-f:1")}
    parse 1 out of -f=1                                         ${longParser("-f=1")}
    parse 1 out of --foo 0x01                                   ${longParser("--foo", "0x01")}
    parse 1 out of --foo:0x01                                   ${longParser("--foo:0x01")}
    parse 1 out of --foo=0x01                                   ${longParser("--foo=0x01")}
    parse 1 out of -f 0x1                                       ${longParser("-f", "0x1")}
    parse 1 out of -f:0x1                                       ${longParser("-f:0x1")}

  opt[String]("foo") action { x => x } should
    parse "bar" out of --foo bar                                ${stringParser("--foo", "bar")}
    parse "bar" out of --foo:bar                                ${stringParser("--foo:bar")}
    parse "bar" out of --foo=bar                                ${stringParser("--foo=bar")}

  opt[Char]("foo") action { x => x } should
    parse 'b' out of --foo b                                    ${charParser("--foo", "b")}
    parse 'b' out of --foo:b                                    ${charParser("--foo:b")}
    parse 'b' out of --foo=b                                    ${charParser("--foo=b")}
    fail to parse --foo bar                                     ${charParserFail("--foo", "bar")}
    fail to parse --foo=bar                                     ${charParserFail("--foo=bar")}

  opt[Double]("foo") action { x => x } should
    parse 1.0 out of --foo 1.0                                  ${doubleParser("--foo", "1.0")}
    parse 1.0 out of --foo:1.0                                  ${doubleParser("--foo:1.0")}
    parse 1.0 out of --foo=1.0                                  ${doubleParser("--foo=1.0")}
    fail to parse --foo bar                                     ${doubleParserFail("--foo", "bar")}
    fail to parse --foo=bar                                     ${doubleParserFail("--foo=bar")}

  opt[Boolean]("foo") action { x => x } should
    parse true out of --foo true                                ${trueParser("--foo", "true")}
    parse true out of --foo:true                                ${trueParser("--foo:true")}
    parse true out of --foo=true                                ${trueParser("--foo=true")}
    parse true out of --foo 1                                   ${trueParser("--foo", "1")}
    parse true out of --foo:1                                   ${trueParser("--foo:1")}
    fail to parse --foo bar                                     ${boolParserFail("--foo", "bar")}
    fail to parse --foo=bar                                     ${boolParserFail("--foo=bar")}

  opt[BigDecimal]("foo") action { x => x } should
    parse 1.0 out of --foo 1.0                                  ${bigDecimalParser("--foo", "1.0")}
    parse 1.0 out of --foo=1.0                                  ${bigDecimalParser("--foo=1.0")}
    fail to parse --foo bar                                     ${bigDecimalParserFail("--foo", "bar")}
    fail to parse --foo=bar                                     ${bigDecimalParserFail("--foo=bar")}

  opt[Calendar]("foo") action { x => x } should
    parse 2000-01-01 out of --foo 2000-01-01                    ${calendarParser("--foo", "2000-01-01")}
    parse 2000-01-01 out of --foo=2000-01-01                    ${calendarParser("--foo=2000-01-01")}
    fail to parse --foo bar                                     ${calendarParserFail("--foo", "bar")}
    fail to parse --foo=bar                                     ${calendarParserFail("--foo=bar")}

  opt[File]("foo") action { x => x } should
    parse test.txt out of --foo test.txt                        ${fileParser("--foo", "test.txt")}
    parse test.txt out of --foo=test.txt                        ${fileParser("--foo=test.txt")}

  opt[Duration]("foo") action { x => x } should
    parse 30s out of --foo 30s                                  ${durationParser("--foo", "30s")}
    parse 30s out of --foo=30s                                  ${durationParser("--foo=30s")}

  opt[(String, Int)]("foo") action { x => x } should
    parse ("k", 1) out of --foo k=1                             ${pairParser("--foo", "k=1")}
    parse ("k", 1) out of --foo:k=1                             ${pairParser("--foo:k=1")}
    parse ("k", 1) out of --foo=k=1                             ${pairParser("--foo=k=1")}
    fail to parse --foo                                         ${pairParserFail("--foo")}
    fail to parse --foo bar                                     ${pairParserFail("--foo", "bar")}
    fail to parse --foo k=bar                                   ${pairParserFail("--foo", "k=bar")}
    fail to parse --foo=k=bar                                   ${pairParserFail("--foo=k=bar")}

  opt[Seq[Int]]("foo") action { x => x } should
    parse Seq(1,2,3) out of --foo "1,2,3"                       ${seqParser("--foo","1,2,3")}
    parse Seq(1,2,3) out of "--foo=1,2,3"                       ${seqParser("--foo=1,2,3")}
    fail to parse --foo                                         ${seqParserFail("--foo")}

  opt[Map[String,Boolean]]("foo") action { x => x } should
    parse Map("true" -> true, "false" -> false) out of --foo "true=true,false=false" ${mapParser("--foo","true=true,false=false")}
    parse Map("true" -> true, "false" -> false) out of "--foo=true=true,false=false" ${mapParser("--foo=true=true,false=false")}
    fail to parse --foo                                         ${mapParserFail("foo")}

  opt[Seq[(String,String)]]("foo") action { x => x } should
    parse Map("key" -> "1", "key" -> "2") out of --foo "key=1,false=false" ${seqTupleParser("--foo","key=1,key=2")}
    fail to parse --foo                                         ${seqTupleParserFail("foo")}

  opt[String]("foo") required() action { x => x } should
    fail to parse Nil                                           ${requiredFail()}

  opt[Unit]("debug") hidden() action { x => x } should
    parse () out of --debug                                     ${unitParserHidden("--debug")}

  unknown options should
    fail to parse by default                                    ${intParserFail("-z", "bar")}

  opt[(String, Int)]("foo") action { x => x } validate { x =>
    if (x > 0) success else failure("Option --foo must be >0") } should
    fail to parse --foo 0                                       ${validFail("--foo", "0")}

  opt[Unit]('f', "foo") action { x => x }; checkConfig { c => if (c.flag) success else failure("flag is false") } should
    parse () out of --foo                                       ${checkSuccess("--foo")}
    fail to parse empty                                         ${checkFail()}

  arg[Int]("<port>") action { x => x } should
    parse 80 out of 80                                          ${intArg("80")}
    be required and should fail to parse Nil                    ${intArgFail()}

  arg[String]("<a>"); arg[String]("<b>") action { x => x } should
    parse "b" out of a b                                        ${multipleArgs("a", "b")}

  arg[String]("<a>") action { x => x} unbounded() optional(); arg[String]("<b>") optional() should
    parse "b" out of a b                                        ${unboundedArgs("a", "b")}
    parse nothing out of Nil                                    ${emptyArgs()}

  cmd("update") action { x => x } children( opt[Unit]("foo") action { x => x} ) should
    parse () out of update                                      ${cmdParser("update")}
    parse () out of update --foo                                ${cmdParser("update", "--foo")}
    fail to parse --foo                                         ${cmdParserFail("--foo")}

  arg[String]("<a>") action { x => x}; cmd("update") children(arg[String]("<b>"), arg[String]("<c>")) ; cmd("commit") should
    parse commit out of update foo bar commit                   ${cmdPosParser("update", "foo", "bar", "commit")}
    parse commit out of commit commit                           ${cmdPosParser("commit", "commit")}
    fail to parse foo update                                    ${cmdPosParserFail("foo", "update")}

  cmd("backend") children( cmd("update") children(arg[String]("<a>") action { x => x} )) should
    parse foo out of backend update foo                         ${nestedCmdParser("backend", "update", "foo")}
    fail to paser backend foo                                   ${nestedCmdParserFail("backend", "foo")}

  help("help") if OneColumn should
    print usage text --help                                     ${helpParserOneColumn()}

  help("help") if TwoColumns should
    print usage text --help                                     ${helpParserTwoColumns()}

  reportError("foo") should
    print "Error: foo\n"                                        ${reportErrorParser("foo")}

  reportWarning("foo") should
    print "Warning: foo\n"                                      ${reportWarningParser("foo")}

  showHeader should
    print "scopt 3.x\n"                                         ${showHeaderParser()}

  showUsage should
    print usage text                                            ${showUsageParser()}

  cmd("update") hidden() children(opt[Unit]("foo").text("foo")) should
    print usage text                                            ${showUsageHiddenCmdParser()}

  terminationSafeParser should
    not terminate on `--help`                                   ${terminationSafeParser("--help")}
    not terminate on `--version`                                ${terminationSafeParser("--version")}

  emptyParser.showUsage
    print empty usage text                                      ${noOptionTest()}
                                                                """
  // opt[URI]("foo") action { x => x } should
  //   parse http://github.com/ out of --foo http://github.com/    ${uriParser("--foo", "http://github.com/")}
  //   parse http://github.com/ out of --foo=http://github.com/    ${uriParser("--foo=http://github.com/")}

  // opt[InetAddress]("foo") action { x => x } should
  //   parse 8.8.8.8 out of --foo 8.8.8.8                          ${inetAddressParser("--foo", "8.8.8.8")}
  //   parse 8.8.8.8 out of --foo=8.8.8.8                          ${inetAddressParser("--foo=8.8.8.8")}


  import SpecUtil._

  val unitParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Unit]('f', "foo").action( (x, c) => c.copy(flag = true) )
    opt[Unit]("debug").action( (x, c) => c.copy(debug = true) )
    help("help")
  }
  def unitParser(args: String*) = {
    val result = unitParser1.parse(args.toSeq, Config())
    result.get.flag === true
  }
  def unitParserHidden(args: String*) = {
    val result = unitParser1.parse(args.toSeq, Config())
    result.get.debug === true
  }

  val groupParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Unit]('a', "alice")
    opt[Unit]('b', "bob")
    opt[Unit]("alicebob").abbr("ab").action( (x, c) => c.copy(flag = true) )
    help("help")
  }
  def groupParser(args: String*) = {
    val result = groupParser1.parse(args.toSeq, Config())
    result.get.flag === true
  }

  val intParser1 = new scopt.OptionParser[Config]("scopt") {
    override def showUsageOnError = true
    head("scopt", "3.x")
    opt[Int]('f', "foo").action( (x, c) => c.copy(intValue = x) )
    help("help")
  }
  def intParser(args: String*) = {
    val result = intParser1.parse(args.toSeq, Config())
    result.get.intValue === 1
  }
  def intParserFail(args: String*) = {
    val result = intParser1.parse(args.toSeq, Config())
    result === None
  }

  val longParser1 = new scopt.OptionParser[Config]("scopt") {
    override def showUsageOnError = true
    head("scopt", "3.x")
    opt[Long]('f', "foo").action( (x, c) => c.copy(longValue = x))
    help("help")
  }
  def longParser(args: String*) = {
    val result = intParser1.parse(args.toSeq, Config())
    result.get.intValue === 1
  }

  val stringParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[String]("foo").action( (x, c) => c.copy(stringValue = x) )
    help("help")
  }
  def stringParser(args: String*) = {
    val result = stringParser1.parse(args.toSeq, Config())
    result.get.stringValue === "bar"
  }

  val charParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Char]("foo").action( (x, c) => c.copy(charValue = x) )
    help("help")
  }
  def charParser(args: String*) = {
    val result = charParser1.parse(args.toSeq, Config())
    result.get.charValue === 'b'
  }
  def charParserFail(args: String*) = {
    val result = charParser1.parse(args.toSeq, Config())
    result === None
  }

  val doubleParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Double]("foo").action( (x, c) => c.copy(doubleValue = x) )
    help("help")
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
    opt[Boolean]("foo").action( (x, c) => c.copy(boolValue = x) )
    help("help")
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
    opt[BigDecimal]("foo").action( (x, c) => c.copy(bigDecimalValue = x) )
    help("help")
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
    opt[Calendar]("foo").action( (x, c) => c.copy(calendarValue = x) )
    help("help")
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
    opt[File]("foo").action( (x, c) => c.copy(fileValue = x) )
    help("help")
  }
  def fileParser(args: String*) = {
    val result = fileParser1.parse(args.toSeq, Config())
    result.get.fileValue === new File("test.txt")
  }

  // val uriParser1 = new scopt.OptionParser[Config]("scopt") {
  //   head("scopt", "3.x")
  //   opt[URI]("foo").action( (x, c) => c.copy(uriValue = x) )
  //   help("help")
  // }
  // def uriParser(args: String*) = {
  //   val result = uriParser1.parse(args.toSeq, Config())
  //   result.get.uriValue === new URI("http://github.com/")
  // }

  val inetAddressParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[InetAddress]("foo").action( (x, c) => c.copy(inetAddressValue = x) )
    help("help")
  }
  def inetAddressParser(args: String*) = {
    val result = inetAddressParser1.parse(args.toSeq, Config())
    result.get.inetAddressValue === InetAddress.getByName("8.8.8.8")
  }

  val durationParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Duration]("foo").action( (x, c) => c.copy(durationValue = x) )
    help("help")
  }
  def durationParser(args: String*) = {
    val result = durationParser1.parse(args.toSeq, Config())
    result.get.durationValue.toMillis === 30000L
  }

  val pairParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[(String, Int)]("foo").action({
     case ((k, v), c) => c.copy(key = k, intValue = v) 
    })
    help("help")
  }
  def pairParser(args: String*) = {
    val result = pairParser1.parse(args.toSeq, Config())
    (result.get.key === "k") and (result.get.intValue === 1)
  }
  def pairParserFail(args: String*) = {
    val result = pairParser1.parse(args.toSeq, Config())
    result === None
  }

  val seqParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Seq[Int]]("foo").action({
      case (s, c) => c.copy(seqInts = s)
    })
    help("help")
  }
  def seqParser(args: String*) = {
    val result = seqParser1.parse(args.toSeq, Config())
    result.get.seqInts === Seq(1,2,3)
  }
  def seqParserFail(args: String*) = {
    val result = seqParser1.parse(args.toSeq, Config())
    result === None
  }

  val mapParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Map[String,Boolean]]("foo").action({
      case (s, c) => c.copy(mapStringToBool = s)
    })
    help("help")
  }
  def mapParser(args: String*) = {
    val result = mapParser1.parse(args.toSeq, Config())
    result.get.mapStringToBool === Map("true" -> true,"false" -> false)
  }
  def mapParserFail(args: String*) = {
    val result = mapParser1.parse(args.toSeq, Config())
    result === None
  }
  val seqTupleParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Seq[(String,String)]]("foo").action({
      case (s, c) => c.copy(seqTupleStringString = s)
    })
    help("help")
  }
  def seqTupleParser(args: String*) = {
    val result = seqTupleParser1.parse(args.toSeq, Config())
    result.get.seqTupleStringString === List("key" -> "1","key" -> "2")
  }
  def seqTupleParserFail(args: String*) = {
    val result = seqTupleParser1.parse(args.toSeq, Config())
    result === None
  }

  //parse Map("true" -> true, "false" -> false) out of --foo "true=true,false=false" ${mapParser("--foo","true=true,false=false")}

  val requireParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[String]("foo").required().action( (x, c) => c.copy(stringValue = x) )
    help("help")
  }
  def requiredFail(args: String*) = {
    val result = requireParser1.parse(args.toSeq, Config())
    result === None
  }

  val validParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Int]('f', "foo").action( (x, c) => c.copy(intValue = x) ).
      validate( x =>
        if (x > 0) success
        else failure("Option --foo must be >0") ).
      validate( x => failure("Just because") )
    help("help")
  }
  def validFail(args: String*) = {
    val result = validParser1.parse(args.toSeq, Config())
    result === None
  }

  val checkParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Unit]('f', "foo").action( (x, c) => c.copy(flag = true) )
    checkConfig { c => if (c.flag) success else failure("flag is false") }
    help("help")
  }
  def checkSuccess(args: String*) = {
    val result = checkParser1.parse(args.toSeq, Config())
    result.get.flag === true
  }
  def checkFail(args: String*) = {
    val result = checkParser1.parse(args.toSeq, Config())
    result === None
  }

  val intArgParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    arg[Int]("<port>").action( (x, c) => c.copy(intValue = x) )
    help("help")
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
    arg[String]("<a>").action( (x, c) => c.copy(a = x) )
    arg[String]("<b>").action( (x, c) => c.copy(b = x) )
    help("help")
  }
  def multipleArgs(args: String*) = {
    val result = multipleArgsParser1.parse(args.toSeq, Config())
    (result.get.a === "a") and (result.get.b === "b")
  }

  val unboundedArgsParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    arg[String]("<a>").action( (x, c) => c.copy(a = x) ).unbounded().optional()
    arg[String]("<b>").action( (x, c) => c.copy(b = x) ).optional()
    help("help")
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
    cmd("update").action( (x, c) => c.copy(flag = true) ).children(
      opt[Unit]("foo").action( (x, c) => c.copy(stringValue = "foo") )
    )
    help("help")
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
    arg[String]("<a>").action( (x, c) => c.copy(a = x) )
    cmd("update").action( (x, c) => c.copy(flag = true) ).children(
      arg[String]("<b>").action( (x, c) => c.copy(b = x) ),
      arg[String]("<c>")
    )
    cmd("commit")
    help("help")
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
    cmd("backend").text("commands to manipulate backends:\n").
      action( (x, c) => c.copy(flag = true) ).
      children(
        cmd("update").children(
          arg[String]("<a>").action( (x, c) => c.copy(a = x) ),
          checkConfig( c =>
            if (c.a == "foo") success
            else failure("not foo") )
        )
    )
    help("help")
  }
  def nestedCmdParser(args: String*) = {
    val result = nestedCmdParser1.parse(args.toSeq, Config())
    result.get.a === "foo"
  }
  def nestedCmdParserFail(args: String*) = {
    val result = nestedCmdParser1.parse(args.toSeq, Config())
    result === None
  }

  def helpParserOneColumn(args: String*) = {
    case class Config(foo: Int = -1, out: File = new File("."), xyz: Boolean = false,
      libName: String = "", maxCount: Int = -1, verbose: Boolean = false, debug: Boolean = false,
      mode: String = "", files: Seq[File] = Seq(), keepalive: Boolean = false,
      jars: Seq[File] = Seq(), kwargs: Map[String,String] = Map())
    val parser = new scopt.OptionParser[Config]("scopt") {
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

    (parser.header === expectedHeader) and (parser.usage === expectedUsage)
  }

  def helpParserTwoColumns(args: String*) = {
    case class Config(foo: Int = -1, out: File = new File("."), xyz: Boolean = false,
      libName: String = "", maxCount: Int = -1, verbose: Boolean = false, debug: Boolean = false,
      mode: String = "", files: Seq[File] = Seq(), keepalive: Boolean = false,
      jars: Seq[File] = Seq(), kwargs: Map[String,String] = Map())
    val parser = new scopt.OptionParser[Config]("scopt") {
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
    parser.parse(args.toSeq, Config())
    val expectedUsage= """scopt 3.x
Usage: scopt [update] [options] [<file>...]

  -f, --foo <value>        foo is an integer property
  -o, --out <file>         out is a required file property
  --max:<libname>=<max>    maximum count for <libname>
  -j, --jars <jar1>,<jar2>...
                           jars to include
  --kwargs k1=v1,k2=v2...  other arguments
  --verbose                verbose is a flag
  --help                   prints this usage text
  <file>...                optional unbounded args
some notes.

Command: update [options]
update is a command.
  -nk, --not-keepalive     disable keepalive
  --xyz <value>            xyz is a boolean property""".newlines
    val expectedHeader = """scopt 3.x"""

    (parser.header === expectedHeader) and (parser.usage === expectedUsage)
  }

  val printParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    help("help") text("prints this usage text")
  }

  val printHiddenCmdParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    cmd("update").hidden().children(
      opt[Unit]("foo").text("foo")
    )
    help("help") text("prints this usage text")
  }

  lazy val terminationSafeParser1 = new scopt.OptionParser[Config]("scopt") {
    override def terminate(exitState: Either[String, Unit]): Unit = ()
    version("version")
    opt[Unit]("debug") action { (x, c) => c.copy(debug = true) }
    help("help") text("prints this usage text")
  }

  def terminationSafeParser(args: String*) = {
    val result = terminationSafeParser1.parse(args.toSeq, Config())
    result.isDefined
  }

  def printParserError(body: scopt.OptionParser[Config] => Unit): String = {
    val errStream = new ByteArrayOutputStream()
    Console.withErr(errStream) { body(printParser1) }
    errStream.toString("UTF-8")
  }
  def printParserOut(thunk: => Unit): String = {
    val outStream = new ByteArrayOutputStream()
    Console.withOut(outStream) { thunk }
    outStream.toString("UTF-8")
  }
  def printParserOut(body: scopt.OptionParser[Config] => Unit): String = {
    printParserOut { body(printParser1) }
  }
  def reportErrorParser(msg: String) = {
    printParserError(_.reportError(msg)) === "Error: foo".newline
  }
  def reportWarningParser(msg: String) = {
    printParserError(_.reportWarning(msg)) === "Warning: foo".newline
  }
  def showHeaderParser() = {
    printParserOut(_.showHeader()) === "scopt 3.x".newline
  }
  def showUsageParser() = {
    printParserOut(_.showUsage()) === """scopt 3.x
Usage: scopt [options]

  --help  prints this usage text
"""
  }

  def showUsageHiddenCmdParser() = {
    printParserOut(printHiddenCmdParser1.showUsage()) === """scopt 3.x
Usage: scopt [update] [options]

  --help  prints this usage text
"""
  }
  
  def noOptionTest() = {
    val emptyParser =
      new scopt.OptionParser[Config]("scopt") {}
    emptyParser.usage !== ""
  }
  

  case class Config(flag: Boolean = false, intValue: Int = 0, longValue: Long = 0L, stringValue: String = "",
    doubleValue: Double = 0.0, boolValue: Boolean = false, debug: Boolean = false,
    bigDecimalValue: BigDecimal = BigDecimal("0.0"),
    calendarValue: Calendar = new GregorianCalendar(1900, Calendar.JANUARY, 1),
    fileValue: File = new File("."),
    uriValue: URI = new URI("http://localhost"),
    inetAddressValue: InetAddress = InetAddress.getByName("0.0.0.0"),
    durationValue: Duration = Duration("0s"),
    key: String = "", a: String = "", b: String = "",
    seqInts: Seq[Int] = Seq(),
    mapStringToBool: Map[String,Boolean] = Map(),
    seqTupleStringString: Seq[(String, String)] = Nil, charValue: Char = 0)
}
