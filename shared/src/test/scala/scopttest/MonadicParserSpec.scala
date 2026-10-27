package scopttest

import scala.concurrent.duration.Duration
import scopt.OParser
import scopt.OEffect._
import SpecUtil._

object MonadicParserSpec extends verify.BasicTestSuite {
  val NL = System.getProperty("line.separator")

  test("programName(s) should generate usage text") {
    val builder = OParser.builder[Config]
    val programName1: OParser[Unit, Config] = {
      import builder._
      programName("scopt")
    }
    assert(
      OParser.usage(programName1) ==
        """Usage: scopt
        |
        |""".stripMargin
    )
    ()
  }

  test("head(s, ...) should generate usage text") {
    val builder = OParser.builder[Config]
    val head1: OParser[Unit, Config] = {
      import builder._
      head("scopt", "3.x")
    }
    assert(
      OParser.usage(head1) ==
        """scopt 3.x
        |""".stripMargin
    )
    ()
  }

  test("head(s, ...) should compose using ++") {
    val builder = OParser.builder[Config]
    val head1: OParser[Unit, Config] = {
      import builder._
      head("scopt", "4.x")
    }
    val head2: OParser[Unit, Config] = {
      import builder._
      head("x", "y")
    }
    val p = head1 ++ head2
    assert(
      OParser.usage(p) ==
        """scopt 4.x
        |x y
        |""".stripMargin
    )
    ()
  }

  test("head(s, ...) should compose using for comprehension") {
    val builder = OParser.builder[Config]
    val programName1: OParser[Unit, Config] = {
      import builder._
      programName("scopt")
    }
    val head1: OParser[Unit, Config] = {
      import builder._
      head("scopt", "4.x")
    }
    val p: OParser[?, Config] = for {
      _ <- programName1
      _ <- head1
    } yield ()
    assert(
      OParser.usage(p) ==
        """scopt 4.x
        |Usage: scopt
        |
        |""".stripMargin
    )
    ()
  }

  test("OParser.sequence should compose OParsers") {
    val builder = OParser.builder[Config]
    import builder._
    val p1 =
      OParser.sequence(
        opt[Int]('f', "foo")
          .action((x, c) => c.copy(intValue = x))
          .text("foo is an integer property"),
        opt[Unit]("debug")
          .action((_, c) => c.copy(debug = true))
          .text("debug is a flag")
      )
    val p2 =
      OParser.sequence(
        arg[String]("<source>")
          .action((x, c) => c.copy(a = x)),
        arg[String]("<dest>")
          .action((x, c) => c.copy(b = x))
      )
    val p =
      OParser.sequence(
        head("scopt", "4.x"),
        programName("scopt"),
        p1,
        p2
      )
    assert(
      OParser.usage(p) ==
        """scopt 4.x
        |Usage: scopt [options] <source> <dest>
        |
        |  -f, --foo <value>  foo is an integer property
        |  --debug            debug is a flag
        |  <source>
        |  <dest>""".stripMargin
    )
    ()
  }

  test("compose configuration type") {
    trait ConfigLike1[R] {
      def withDebug(value: Boolean): R
    }
    def parser1[R <: ConfigLike1[R]]: OParser[?, R] = {
      val builder = OParser.builder[R]
      import builder._
      OParser.sequence(
        opt[Unit]("debug").action((_, c) => c.withDebug(true)),
        note("something")
      )
    }

    trait ConfigLike2[R] {
      def withVerbose(value: Boolean): R
    }
    def parser2[R <: ConfigLike2[R]]: OParser[?, R] = {
      val builder = OParser.builder[R]
      import builder._
      OParser.sequence(
        opt[Unit]("verbose").action((_, c) => c.withVerbose(true)),
        note("something else")
      )
    }
    case class Config1(debug: Boolean = false, verbose: Boolean = false)
        extends ConfigLike1[Config1]
        with ConfigLike2[Config1] {
      override def withDebug(value: Boolean) = copy(debug = value)
      override def withVerbose(value: Boolean) = copy(verbose = value)
    }
    val parser3: OParser[?, Config1] = {
      val builder = OParser.builder[Config1]
      import builder._
      OParser.sequence(
        programName("scopt"),
        head("scopt", "4.x"),
        parser1,
        parser2
      )
    }
    val result = OParser.parse(parser3, Array("--verbose"), Config1())
    assert(result.get.verbose == true)
    ()
  }

  test("OParser.sequence should allow duplicates") {
    val builder = OParser.builder[Config]
    val parser1: OParser[Unit, Config] = {
      import builder._
      opt[Unit]('b', "bob")
    }
    val p: OParser[?, Config] = {
      import builder._
      OParser.sequence(
        head("scopt", "4.x"),
        programName("scopt"),
        parser1.text("text"),
        parser1
      )
    }
    assert(
      OParser.usage(p) ==
        """scopt 4.x
        |Usage: scopt [options]
        |
        |  -b, --bob  text
        |  -b, --bob""".stripMargin
    )
    ()
  }

  test("unit parser should generate usage") {
    val builder = OParser.builder[Config]
    val unitParser1: OParser[?, Config] = {
      import builder._
      OParser.sequence(
        programName("scopt"),
        head("scopt", "4.x"),
        opt[Unit]('f', "foo").action((x, c: Config) => c.copy(flag = true))
      )
    }
    assert(
      OParser.usage(unitParser1) ==
        """scopt 4.x
        |Usage: scopt [options]
        |
        |  -f, --foo""".stripMargin
    )
    ()
  }

  test("unit parser should parse ()") {
    val builder = OParser.builder[Config]
    val parser: OParser[?, Config] = {
      import builder._
      OParser.sequence(
        programName("scopt"),
        head("scopt", "4.x"),
        opt[Unit]('f', "foo").action((x, c: Config) => c.copy(flag = true))
      )
    }
    def unitParser(args: String*): Unit = {
      val result = OParser.parse(parser, args.toSeq, Config())
      assert(result.get.flag == true)
    }
    unitParser("--foo")
    unitParser("-f")
  }

  test("options should generate usage text") {
    val builder = OParser.builder[Config]
    val parser: OParser[?, Config] = {
      import builder._
      OParser.sequence(
        programName("scopt"),
        head("scopt", "4.x"),
        opt[Unit]('a', "alice"),
        opt[Unit]('b', "bob"),
        opt[Unit]("alicebob").abbr("ab").action((x, c) => c.copy(flag = true))
      )
    }
    assert(
      OParser.usage(parser) ==
        """scopt 4.x
        |Usage: scopt [options]
        |
        |  -a, --alice
        |  -b, --bob
        |  -ab, --alicebob""".stripMargin
    )
    ()
  }

  test("grouped parser should parse ()") {
    val builder = OParser.builder[Config]
    val parser: OParser[?, Config] = {
      import builder._
      OParser.sequence(
        programName("scopt"),
        head("scopt", "4.x"),
        opt[Unit]('a', "alice"),
        opt[Unit]('b', "bob"),
        opt[Unit]("alicebob").abbr("ab").action((x, c) => c.copy(flag = true))
      )
    }
    def groupParser(args: String*): Unit = {
      val result = OParser.parse(parser, args.toSeq, Config())
      assert(result.get.flag)
    }
    groupParser("-ab")
    groupParser("-abab")
  }

  test("int parser should generate usage") {
    val builder = OParser.builder[Config]
    val parser: OParser[?, Config] = {
      import builder._
      OParser.sequence(
        programName("scopt"),
        head("scopt", "4.x"),
        opt[Int]('f', "foo").action((x, c) => c.copy(intValue = x)),
        help("help")
      )
    }
    assert(
      OParser.usage(parser) ==
        """scopt 4.x
        |Usage: scopt [options]
        |
        |  -f, --foo <value>
        |  --help""".stripMargin
    )
    ()
  }

  test("int parser should parse 1") {
    val builder = OParser.builder[Config]
    val parser: OParser[?, Config] = {
      import builder._
      OParser.sequence(
        programName("scopt"),
        head("scopt", "4.x"),
        opt[Int]('f', "foo").action((x, c) => c.copy(intValue = x)),
        help("help")
      )
    }
    def intParser(args: String*): Unit = {
      val result = OParser.parse(parser, args.toSeq, Config())
      assert(result.get.intValue == 1)
    }
    def intParserFail(args: String*): Unit = {
      val result = OParser.parse(parser, args.toSeq, Config())
      assert(result == None)
    }
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
    intParserFail { "--foo" }
    intParserFail("--foo", "bar")
    intParserFail("--foo=bar")
  }

  test("BigDecimal parser should generate usage") {
    val builder = OParser.builder[Config]
    val parser: OParser[?, Config] = {
      import builder._
      OParser.sequence(
        programName("scopt"),
        head("scopt", "4.x"),
        opt[BigDecimal]('f', "foo").action((x, c) => c.copy(bigDecimalValue = x)),
        help("help")
      )
    }
    assert(
      OParser.usage(parser) ==
        """scopt 4.x
        |Usage: scopt [options]
        |
        |  -f, --foo <value>
        |  --help""".stripMargin
    )
    ()
  }

  test("BigDecimal parser should parse 1.0") {
    val builder = OParser.builder[Config]
    val parser: OParser[?, Config] = {
      import builder._
      OParser.sequence(
        programName("scopt"),
        head("scopt", "4.x"),
        opt[BigDecimal]('f', "foo").action((x, c) => c.copy(bigDecimalValue = x)),
        help("help")
      )
    }
    def bigDecimalParser(args: String*): Unit = {
      val result = OParser.parse(parser, args.toSeq, Config())
      assert(result.get.bigDecimalValue == BigDecimal("1.0"))
    }
    def bigDecimalParserFail(args: String*): Unit = {
      val result = OParser.parse(parser, args.toSeq, Config())
      assert(result == None)
    }
    bigDecimalParser("--foo", "1.0")
    bigDecimalParser("--foo=1.0")
    bigDecimalParserFail("--foo", "bar")
    bigDecimalParserFail("--foo=bar")
  }

  test("""opt[String]("foo").required() action { x => x }""" + " should fail to parse Nil") {
    val builder = OParser.builder[Config]
    val parser: OParser[?, Config] = {
      import builder._
      OParser.sequence(
        programName("scopt"),
        head("scopt", "4.x"),
        opt[String]("foo").required().action((x, c) => c.copy(stringValue = x)),
        help("help")
      )
    }
    val result = OParser.parse(parser, Nil, Config())
    assert(result == None)
    ()
  }

  test("custom validation") {
    val builder = OParser.builder[Config]
    val parser: OParser[?, Config] = {
      import builder._
      OParser.sequence(
        programName("scopt"),
        head("scopt", "4.x"),
        opt[Int]('f', "foo")
          .action((x, c) => c.copy(intValue = x))
          .validate(x =>
            if (x > 0) success
            else failure("Option --foo must be >0")
          )
          .validate(x => failure("Just because"))
      )
    }
    val result = OParser.parse(parser, List("--foo", "0"), Config())
    assert(result == None)
    ()
  }

  test("command usage") {
    val builder = OParser.builder[Config]
    val parser: OParser[?, Config] = {
      import builder._
      OParser.sequence(
        programName("scopt"),
        head("scopt", "4.x"),
        cmd("update")
          .action((x, c) => c.copy(flag = true))
          .children(
            OParser.sequence(
              opt[Unit]("foo").action((x, c) => c.copy(stringValue = "foo")),
              opt[Int]("bar").action((x, c) => c.copy(intValue = x))
            )
          )
      )
    }
    assert(
      OParser.usage(parser) ==
        """scopt 4.x
        |Usage: scopt [update]
        |
        |Command: update [options]
        |
        |  --foo
        |  --bar <value>""".stripMargin
    )
    ()
  }

  test("option parser can be reused across multiple commands") {
    val builder = OParser.builder[Config]
    val suboptionParser1: OParser[?, Config] = {
      import builder._
      OParser.sequence(
        opt[Unit]("foo").action((x, c) => c.copy(stringValue = "foo")),
        opt[Int]("bar").action((x, c) => c.copy(intValue = x)),
        note("")
      )
    }
    val parser: OParser[Unit, Config] = {
      import builder._
      OParser.sequence(
        programName("scopt"),
        head("scopt", "4.x"),
        cmd("update")
          .action((x, c) => c.copy(flag = true))
          .children(suboptionParser1),
        cmd("status")
          .action((x, c) => c.copy(flag = true))
          .children(suboptionParser1)
      )
    }
    assert(
      OParser.usage(parser) ==
        """scopt 4.x
        |Usage: scopt [update|status]
        |
        |Command: update [options]
        |
        |  --foo
        |  --bar <value>
        |
        |Command: status [options]
        |
        |  --foo
        |  --bar <value>
        |""".stripMargin
    )
    ()
  }

  test("--version should display header") {
    val builder = OParser.builder[Config]
    val parser: OParser[?, Config] = {
      import builder._
      OParser.sequence(
        programName("scopt"),
        head("scopt", "4.x"),
        version("version")
      )
    }
    val out = printParserOut {
      OParser.parse(
        parser,
        List("--version"),
        Config(),
        new scopt.DefaultOEffectSetup {
          override def terminate(exitState: Either[String, Unit]): Unit = ()
        }
      )
    }
    assert(out == "scopt 4.x".newline)
    ()
  }

  test("--help should display the usage text") {
    val builder = OParser.builder[Config]
    val parser: OParser[?, Config] = {
      import builder._
      OParser.sequence(
        programName("scopt"),
        head("scopt", "4.x"),
        help("help").text("prints this usage text")
      )
    }
    val out = printParserOut {
      OParser.parse(
        parser,
        List("--help"),
        Config(),
        new scopt.DefaultOEffectSetup {
          override def terminate(exitState: Either[String, Unit]): Unit = ()
        }
      )
    }
    assert(
      out ==
        """scopt 4.x
        |Usage: scopt [options]
        |
        |  --help  prints this usage text
        |""".stripMargin
    )
    ()
  }

  test("empty children list should not throw exception") {
    val builder = OParser.builder[Config]
    val parser: OParser[?, Config] = {
      import builder._
      OParser.sequence(
        programName("scopt"),
        cmd("issue_271").children()
      )
    }
    // run parse with no exception
    OParser.parse(parser, List("issue_271"), Config())
    ()
  }

  test("showMultilineUsage") {
    val builder = OParser.builder[Config]

    val parser: OParser[?, Config] = {
      import builder._
      OParser.sequence(
        programName("scopt"),
        head("scopt", "4.x"),
        help("help").text(
          """|prints this usage text
             |here's a second line of text""".stripMargin
        )
      )
    }
    val (_, effects) =
      OParser.runParser(parser, List("--help"), Config())

    assert(
      effects.head ==
        DisplayToOut(
          """|scopt 4.x
           |Usage: scopt [options]
           |
           |  --help  prints this usage text
           |          here's a second line of text""".stripMargin
        )
    )
    ()
  }

  case class Config(
      flag: Boolean = false,
      intValue: Int = 0,
      longValue: Long = 0L,
      stringValue: String = "",
      doubleValue: Double = 0.0,
      boolValue: Boolean = false,
      debug: Boolean = false,
      bigDecimalValue: BigDecimal = BigDecimal("0.0"),
      durationValue: Duration = Duration("0s"),
      key: String = "",
      a: String = "",
      b: String = "",
      seqInts: Seq[Int] = Seq(),
      mapStringToBool: Map[String, Boolean] = Map(),
      seqTupleStringString: Seq[(String, String)] = Nil,
      charValue: Char = 0
  )
}
