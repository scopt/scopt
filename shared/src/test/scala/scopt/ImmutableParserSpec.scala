import verify._
import java.io.ByteArrayOutputStream
import java.net.URI

import scala.concurrent.duration.Duration

object ImmutableParserSpec extends BasicTestSuite {
  test("unit parser should parse ()") {
    unitParser("--foo")
    unitParser("-f")
  }

  test("grouped parser should parse ()") {
    groupParser("-ab")
    groupParser("-abab")
  }

  test("int parser should parse 1") {
    intParser("--foo", "1")
    intParser("--foo:1")
    intParser("--foo=1")
    intParser("-f", "1")
    intParser("-f:1")
    intParser("-f=1")
    intParser("--foo", "0x01")
    intParser("--foo:0x01")
    intParser("--foo=0x01")
    intParser("-f", "0x1")
    intParser("-f:0x1")
    intParserFail{"--foo"}
    intParserFail("--foo", "bar")
    intParserFail("--foo=bar")
  }

  test("long parser should parse 1") {
    longParser("--foo", "1")
    longParser("--foo:1")
    longParser("--foo=1")
    longParser("-f", "1")
    longParser("-f:1")
    longParser("-f=1")
    longParser("--foo", "0x01")
    longParser("--foo:0x01")
    longParser("--foo=0x01")
    longParser("-f", "0x1")
    longParser("-f:0x1")
  }

  test("string parser should parse bar") {
    stringParser("--foo", "bar")
    stringParser("--foo:bar")
    stringParser("--foo=bar")
  }

  test("char parser should parse 'b'") {
    charParser("--foo", "b")
    charParser("--foo:b")
    charParser("--foo=b")
    charParserFail("--foo", "bar")
    charParserFail("--foo=bar")
  }

  test("double parser should parse 1.0") {
    doubleParser("--foo", "1.0")
    doubleParser("--foo:1.0")
    doubleParser("--foo=1.0")
    doubleParserFail("--foo", "bar")
    doubleParserFail("--foo=bar")
  }

  test("boolean parser should parse true") {
    trueParser("--foo", "true")
    trueParser("--foo:true")
    trueParser("--foo=true")
    trueParser("--foo", "1")
    trueParser("--foo:1")
    boolParserFail("--foo", "bar")
    boolParserFail("--foo=bar")
  }

  test("BigDecimal parser should parse 1.0") {
    bigDecimalParser("--foo", "1.0")
    bigDecimalParser("--foo=1.0")
    bigDecimalParserFail("--foo", "bar")
    bigDecimalParserFail("--foo=bar")
  }

  test("URI parser should parse an URI") {
    uriParser("--foo", "http://github.com/")
    uriParser("--foo=http://github.com/")
  }

  test("Duration parser should parse a Duration") {
    durationParser("--foo", "30s")
    durationParser("--foo=30s")
  }

  test("pair parse should parse (k, 1)") {
    pairParser("--foo", "k=1")
    pairParser("--foo:k=1")
    pairParser("--foo=k=1")
    pairParserFail("--foo")
    pairParserFail("--foo", "bar")
    pairParserFail("--foo", "k=bar")
    pairParserFail("--foo=k=bar")
  }

  test("seq parser should parse Seq(1, 2, 3)") {
    seqParser("--foo","1,2,3")
    seqParser("--foo=1,2,3")
    seqParserFail("--foo")
  }

  test("map parser should parse a map") {
    mapParser("--foo","true=true,false=false")
    mapParser("--foo=true=true,false=false")
    mapParserFail("foo")
  }

  test("seq tuple parser") {
    seqTupleParser("--foo", "key=1,key=2")
    seqTupleParserFail("foo")
  }

  test(".required() should fail when the option is missing") {
    requiredFail()
  }

  test(".required().withFallback() should parse the provided value") {
    requiredWithFallback(args = Seq("--stringValue", "provided"), expected = "provided")
  }

  test(".required().withFallback() should use the fallback value") {
    requiredWithFallback(args = Nil, expected = "someFallback")
  }

  test(".hidden() option should still parse ()") {
    unitParserHidden("--debug")
  }

  test("unknown options should fail to parse by default") {
    intParserFail("-z", "bar")
  }

  test("validate should fail to parse --foo 0") {
    validFail("--foo", "0")
  }

  test("checkConfig") {
    checkSuccess("--foo")
    checkFail()
  }

  test("int argument should parse 80") {
    intArg("80")
    intArgFail()
  }

  test("string argument should parse a string") {
    multipleArgs("a", "b")
  }

  test("unbounded() should parse strings") {
    unboundedArgs("a", "b")
    emptyArgs()
  }

  test("command") {
    cmdParser("update")
    cmdParser("update", "--foo")
    cmdParserFail("--foo")
  }

  test("multiple commands") {
    cmdPosParser("update", "foo", "bar", "commit")
    cmdPosParser("commit", "commit")
    cmdPosParserFail("foo", "update")
  }

  test("nested commands") {
    nestedCmdParser("backend", "update", "foo")
    nestedCmdParserFail("backend", "foo")
  }

  test("help with one column") {
    helpParserOneColumn()
  }

  test("help with two columns") {
    helpParserTwoColumns()
  }

  test("reportError should print error") {
    reportErrorParser("foo")
  }

  test("reportWarning should print warning") {
    reportWarningParser("foo")
  }

  test("showHeader") {
    showHeaderParser()
  }

  test("showUsage") {
    showUsageParser()
  }

  test("hidden command") {
    showUsageHiddenCmdParser()
  }

  test("termination safety") {
    terminationSafeParser("--help")
    terminationSafeParser("--version")
  }

  test("emptyParser.showUsage") {
    noOptionTest()
  }

  test("withFallback doesn't use .toString") {
    val expectedValue: NotToStringInverse = NotToStringInverse("hi", 2)
    val parser = new scopt.OptionParser[ConfigWithNotToStringInverse]("withFallback") {
      head("withFallback")
      opt[NotToStringInverse]('x', "x")
        .withFallback(() => expectedValue)
        .action { case (value, config) => config.copy(x = value) }
    }

    val parsed = parser.parse(Seq(), ConfigWithNotToStringInverse.empty)
    assert(parsed.contains(ConfigWithNotToStringInverse(expectedValue)))
  }

  import SpecUtil._

  val unitParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Unit]('f', "foo").action( (x, c) => c.copy(flag = true) )
    opt[Unit]("debug").action( (x, c) => c.copy(debug = true) )
    help("help")
  }
  def unitParser(args: String*): Unit = {
    val result = unitParser1.parse(args.toSeq, Config())
    assert(result.get.flag == true)
  }
  def unitParserHidden(args: String*): Unit = {
    val result = unitParser1.parse(args.toSeq, Config())
    assert(result.get.debug == true)
  }

  val groupParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Unit]('a', "alice")
    opt[Unit]('b', "bob")
    opt[Unit]("alicebob").abbr("ab").action( (x, c) => c.copy(flag = true) )
    help("help")
  }
  def groupParser(args: String*): Unit = {
    val result = groupParser1.parse(args.toSeq, Config())
    assert(result.get.flag)
  }

  val intParser1 = new scopt.OptionParser[Config]("scopt") {
    override def showUsageOnError = true
    head("scopt", "3.x")
    opt[Int]('f', "foo").action( (x, c) => c.copy(intValue = x) )
    help("help")
  }
  def intParser(args: String*): Unit = {
    val result = intParser1.parse(args.toSeq, Config())
    assert(result.get.intValue == 1)
  }
  def intParserFail(args: String*): Unit = {
    val result = intParser1.parse(args.toSeq, Config())
    assert(result == None)
  }

  val longParser1 = new scopt.OptionParser[Config]("scopt") {
    override def showUsageOnError = true
    head("scopt", "3.x")
    opt[Long]('f', "foo").action( (x, c) => c.copy(longValue = x))
    help("help")
  }
  def longParser(args: String*): Unit = {
    val result = intParser1.parse(args.toSeq, Config())
    assert(result.get.intValue == 1)
  }

  val stringParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[String]("foo").action( (x, c) => c.copy(stringValue = x) )
    help("help")
  }
  def stringParser(args: String*): Unit = {
    val result = stringParser1.parse(args.toSeq, Config())
    assert(result.get.stringValue == "bar")
  }

  val charParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Char]("foo").action( (x, c) => c.copy(charValue = x) )
    help("help")
  }
  def charParser(args: String*): Unit = {
    val result = charParser1.parse(args.toSeq, Config())
    assert(result.get.charValue == 'b')
  }
  def charParserFail(args: String*): Unit = {
    val result = charParser1.parse(args.toSeq, Config())
    assert(result == None)
  }

  val doubleParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Double]("foo").action( (x, c) => c.copy(doubleValue = x) )
    help("help")
  }
  def doubleParser(args: String*): Unit = {
    val result = doubleParser1.parse(args.toSeq, Config())
    assert(result.get.doubleValue == 1.0)
  }
  def doubleParserFail(args: String*): Unit = {
    val result = doubleParser1.parse(args.toSeq, Config())
    assert(result == None)
  }

  val boolParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Boolean]("foo").action( (x, c) => c.copy(boolValue = x) )
    help("help")
  }
  def trueParser(args: String*): Unit = {
    val result = boolParser1.parse(args.toSeq, Config())
    assert(result.get.boolValue)
  }
  def boolParserFail(args: String*): Unit = {
    val result = boolParser1.parse(args.toSeq, Config())
    assert(result == None)
  }

  val bigDecimalParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[BigDecimal]("foo").action( (x, c) => c.copy(bigDecimalValue = x) )
    help("help")
  }
  def bigDecimalParser(args: String*): Unit = {
    val result = bigDecimalParser1.parse(args.toSeq, Config())
    assert(result.get.bigDecimalValue == BigDecimal("1.0"))
  }
  def bigDecimalParserFail(args: String*): Unit = {
    val result = bigDecimalParser1.parse(args.toSeq, Config())
    assert(result == None)
  }

  val uriParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[URI]("foo").action( (x, c) => c.copy(uriValue = x) )
    help("help")
  }
  def uriParser(args: String*): Unit = {
    val result = uriParser1.parse(args.toSeq, Config())
    assert(result.get.uriValue == new URI("http://github.com/"))
  }

  val durationParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Duration]("foo").action( (x, c) => c.copy(durationValue = x) )
    help("help")
  }
  def durationParser(args: String*): Unit = {
    val result = durationParser1.parse(args.toSeq, Config())
    assert(result.get.durationValue.toMillis == 30000L)
  }

  val pairParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[(String, Int)]("foo").action({
     case ((k, v), c) => c.copy(key = k, intValue = v) 
    })
    help("help")
  }
  def pairParser(args: String*): Unit = {
    val result = pairParser1.parse(args.toSeq, Config())
    assert((result.get.key == "k") && (result.get.intValue == 1))
  }
  def pairParserFail(args: String*): Unit = {
    val result = pairParser1.parse(args.toSeq, Config())
    assert(result == None)
  }

  val seqParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Seq[Int]]("foo").action({
      case (s, c) => c.copy(seqInts = s)
    })
    help("help")
  }
  def seqParser(args: String*): Unit = {
    val result = seqParser1.parse(args.toSeq, Config())
    assert(result.get.seqInts == Seq(1, 2, 3))
  }
  def seqParserFail(args: String*): Unit = {
    val result = seqParser1.parse(args.toSeq, Config())
    assert(result == None)
  }

  val mapParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Map[String,Boolean]]("foo").action({
      case (s, c) => c.copy(mapStringToBool = s)
    })
    help("help")
  }
  def mapParser(args: String*): Unit = {
    val result = mapParser1.parse(args.toSeq, Config())
    assert(result.get.mapStringToBool == Map("true" -> true,"false" -> false))
  }
  def mapParserFail(args: String*): Unit = {
    val result = mapParser1.parse(args.toSeq, Config())
    assert(result == None)
  }

  val seqTupleParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Seq[(String,String)]]("foo").action({
      case (s, c) => c.copy(seqTupleStringString = s)
    })
    help("help")
  }
  def seqTupleParser(args: String*): Unit = {
    val result = seqTupleParser1.parse(args.toSeq, Config())
    assert(result.get.seqTupleStringString == List("key" -> "1", "key" -> "2"))
  }
  def seqTupleParserFail(args: String*): Unit = {
    val result = seqTupleParser1.parse(args.toSeq, Config())
    assert(result == None)
  }

  //parse Map("true" -> true, "false" -> false) out of --foo "true=true,false=false" ${mapParser("--foo","true=true,false=false")}

  val requireParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[String]("foo").required().action( (x, c) => c.copy(stringValue = x) )
    help("help")
  }
  def requiredFail(args: String*): Unit = {
    val result = requireParser1.parse(args.toSeq, Config())
    assert(result == None)
  }

  def requiredWithFallback(args: Seq[String], expected: String): Unit =
    assert(new scopt.OptionParser[Config]("scopt") {
      head("scopt", "3.x")
      opt[String]("stringValue").required().withFallback(() => "someFallback")
        .action( (x, c) => c.copy(stringValue = x) )
    }.parse(args, Config()) == Some(Config(stringValue = expected)))

  val validParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Int]('f', "foo").action( (x, c) => c.copy(intValue = x) ).
      validate( x =>
        if (x > 0) success
        else failure("Option --foo must be >0") ).
      validate( x => failure("Just because") )
    help("help")
  }
  def validFail(args: String*): Unit = {
    val result = validParser1.parse(args.toSeq, Config())
    assert(result == None)
  }

  val checkParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    opt[Unit]('f', "foo").action( (x, c) => c.copy(flag = true) )
    checkConfig { c => if (c.flag) success else failure("flag is false") }
    help("help")
  }
  def checkSuccess(args: String*): Unit = {
    val result = checkParser1.parse(args.toSeq, Config())
    assert(result.get.flag)
  }
  def checkFail(args: String*): Unit = {
    val result = checkParser1.parse(args.toSeq, Config())
    assert(result == None)
  }

  val intArgParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    arg[Int]("<port>").action( (x, c) => c.copy(intValue = x) )
    help("help")
  }
  def intArg(args: String*): Unit = {
    val result = intArgParser1.parse(args.toSeq, Config())
    assert(result.get.intValue == 80)
  }
  def intArgFail(args: String*): Unit = {
    val result = intArgParser1.parse(args.toSeq, Config())
    assert(result == None)
  }

  val multipleArgsParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    arg[String]("<a>").action( (x, c) => c.copy(a = x) )
    arg[String]("<b>").action( (x, c) => c.copy(b = x) )
    help("help")
  }
  def multipleArgs(args: String*): Unit = {
    val result = multipleArgsParser1.parse(args.toSeq, Config())
    assert((result.get.a == "a") && (result.get.b == "b"))
  }

  val unboundedArgsParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    arg[String]("<a>").action( (x, c) => c.copy(a = x) ).unbounded().optional()
    arg[String]("<b>").action( (x, c) => c.copy(b = x) ).optional()
    help("help")
  }
  def unboundedArgs(args: String*): Unit = {
    val result = unboundedArgsParser1.parse(args.toSeq, Config())
    assert((result.get.a == "b") && (result.get.b == ""))
  }
  def emptyArgs(args: String*): Unit = {
    val result = unboundedArgsParser1.parse(args.toSeq, Config())
    assert((result.get.a == "") && (result.get.b == ""))
  }

  val cmdParser1 = new scopt.OptionParser[Config]("scopt") {
    head("scopt", "3.x")
    cmd("update").action( (x, c) => c.copy(flag = true) ).children(
      opt[Unit]("foo").action( (x, c) => c.copy(stringValue = "foo") )
    )
    help("help")
  }
  def cmdParser(args: String*): Unit = {
    val result = cmdParser1.parse(args.toSeq, Config())
    assert(result.get.flag)
  }
  def cmdParserFail(args: String*): Unit = {
    val result = cmdParser1.parse(args.toSeq, Config())
    assert(result == None)
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
  def cmdPosParser(args: String*): Unit = {
    val result = cmdPosParser1.parse(args.toSeq, Config())
    assert(result.get.a == "commit")
  }
  def cmdPosParserFail(args: String*): Unit = {
    val result = cmdPosParser1.parse(args.toSeq, Config())
    assert(result == None)
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
  def nestedCmdParser(args: String*): Unit = {
    val result = nestedCmdParser1.parse(args.toSeq, Config())
    assert(result.get.a == "foo")
  }
  def nestedCmdParserFail(args: String*): Unit = {
    val result = nestedCmdParser1.parse(args.toSeq, Config())
    assert(result == None)
  }

  def helpParserOneColumn(args: String*): Unit = {
    case class Config(foo: Int = -1, xyz: Boolean = false,
      libName: String = "", maxCount: Int = -1, verbose: Boolean = false, debug: Boolean = false,
      mode: String = "", keepalive: Boolean = false,
      kwargs: Map[String,String] = Map())
    val parser = new scopt.OptionParser[Config]("scopt") {
      override def renderingMode = scopt.RenderingMode.OneColumn
      head("scopt", "3.x")

      opt[Int]('f', "foo").action( (x, c) =>
        c.copy(foo = x) ).text("foo is an integer property")

      opt[(String, Int)]("max").action({
          case ((k, v), c) => c.copy(libName = k, maxCount = v) }).
        validate( x =>
          if (x._2 > 0) success
          else failure("Value <max> must be >0") ).
        keyValueName("<libname>", "<max>").
        text("maximum count for <libname>")

      opt[Map[String,String]]("kwargs").valueName("k1=v1,k2=v2...").action( (x, c) =>
        c.copy(kwargs = x) ).text("other arguments")

      opt[Unit]("verbose").action( (_, c) =>
        c.copy(verbose = true) ).text("verbose is a flag")

      opt[Unit]("debug").hidden().action( (_, c) =>
        c.copy(debug = true) ).text("this option is hidden in the usage text")

      help("help").text("prints this usage text")

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
Usage: scopt [update] [options]

  -f <value> | --foo <value>
        foo is an integer property
  --max:<libname>=<max>
        maximum count for <libname>
  --kwargs k1=v1,k2=v2...
        other arguments
  --verbose
        verbose is a flag
  --help
        prints this usage text
some notes.

Command: update [options]
update is a command.
  -nk | --not-keepalive
        disable keepalive
  --xyz <value>
        xyz is a boolean property""".newlines
    val expectedHeader = """scopt 3.x"""

    assert((parser.header == expectedHeader) && (parser.usage == expectedUsage))
  }

  def helpParserTwoColumns(args: String*): Unit = {
    case class Config(foo: Int = -1, xyz: Boolean = false,
      libName: String = "", maxCount: Int = -1, verbose: Boolean = false, debug: Boolean = false,
      mode: String = "", keepalive: Boolean = false,
      kwargs: Map[String,String] = Map())
    val parser = new scopt.OptionParser[Config]("scopt") {
      head("scopt", "3.x")

      opt[Int]('f', "foo").action( (x, c) =>
        c.copy(foo = x) ).text("foo is an integer property")

      opt[(String, Int)]("max").action({
          case ((k, v), c) => c.copy(libName = k, maxCount = v) }).
        validate( x =>
          if (x._2 > 0) success
          else failure("Value <max> must be >0") ).
        keyValueName("<libname>", "<max>").
        text("maximum count for <libname>")

      opt[Map[String,String]]("kwargs").valueName("k1=v1,k2=v2...").action( (x, c) =>
        c.copy(kwargs = x) ).text("other arguments")

      opt[Unit]("verbose").action( (_, c) =>
        c.copy(verbose = true) ).text("verbose is a flag")

      opt[Unit]("debug").hidden().action( (_, c) =>
        c.copy(debug = true) ).text("this option is hidden in the usage text")

      help("help").text("prints this usage text")

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
Usage: scopt [update] [options]

  -f, --foo <value>        foo is an integer property
  --max:<libname>=<max>    maximum count for <libname>
  --kwargs k1=v1,k2=v2...  other arguments
  --verbose                verbose is a flag
  --help                   prints this usage text
some notes.

Command: update [options]
update is a command.
  -nk, --not-keepalive     disable keepalive
  --xyz <value>            xyz is a boolean property""".newlines
    val expectedHeader = """scopt 3.x"""

    assert((parser.header == expectedHeader) && (parser.usage == expectedUsage))
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

  def terminationSafeParser(args: String*): Unit = {
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
  def reportErrorParser(msg: String): Unit = {
    assert(printParserError(_.reportError(msg)) == "Error: foo".newline)
  }
  def reportWarningParser(msg: String): Unit = {
    assert(printParserError(_.reportWarning(msg)) == "Warning: foo".newline)
  }
  def showHeaderParser(): Unit = {
    assert(printParserOut(_.showHeader()) == "scopt 3.x".newline)
  }
  def showUsageParser(): Unit = {
    assert(printParserOut(_.showUsage()) == """scopt 3.x
Usage: scopt [options]

  --help  prints this usage text
""")
  }

  def showUsageHiddenCmdParser(): Unit = {
    assert(printParserOut(printHiddenCmdParser1.showUsage()) == """scopt 3.x
Usage: scopt [options]

  --help  prints this usage text
""")
  }

  def noOptionTest(): Unit = {
    val emptyParser =
      new scopt.OptionParser[Config]("scopt") {}
    assert(emptyParser.usage != "")
  }


  case class Config(flag: Boolean = false, intValue: Int = 0, longValue: Long = 0L, stringValue: String = "",
    doubleValue: Double = 0.0, boolValue: Boolean = false, debug: Boolean = false,
    bigDecimalValue: BigDecimal = BigDecimal("0.0"),
    uriValue: URI = new URI("http://localhost"),
    durationValue: Duration = Duration("0s"),
    key: String = "", a: String = "", b: String = "",
    seqInts: Seq[Int] = Seq(),
    mapStringToBool: Map[String,Boolean] = Map(),
    seqTupleStringString: Seq[(String, String)] = Nil, charValue: Char = 0)
}
