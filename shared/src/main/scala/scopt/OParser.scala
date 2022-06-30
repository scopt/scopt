package scopt

import scala.collection.{ Seq => CSeq }

abstract class OParserBuilder[C] {
  def programName(x: String): OParser[Unit, C] =
    wrap(makeDef[Unit](OptionDefKind.ProgramName, "")).text(x)

  /** adds usage text. */
  def head(xs: String*): OParser[Unit, C] =
    wrap(makeDef[Unit](OptionDefKind.Head, "")).text(xs.mkString(" "))

  /** adds an option invoked by `--name x`.
   * @param name name of the option
   */
  def opt[A: Read](name: String): OParser[A, C] =
    wrap(makeDef[A](OptionDefKind.Opt, name))

  /** adds an option invoked by `-x value` or `--name value`.
   * @param x name of the short option
   * @param name name of the option
   */
  def opt[A: Read](x: Char, name: String): OParser[A, C] =
    opt[A](name).abbr(x.toString)

  /** adds usage text. */
  def note(x: String): OParser[Unit, C] =
    wrap(makeDef[Unit](OptionDefKind.Note, "").text(x))

  /** adds an argument invoked by an option without `-` or `--`.
   * @param name name in the usage text
   */
  def arg[A: Read](name: String): OParser[A, C] =
    wrap(makeDef(OptionDefKind.Arg, name)).required()

  /** adds a command invoked by an option without `-` or `--`.
   * @param name name of the command
   */
  def cmd(name: String): OParser[Unit, C] =
    wrap(makeDef[Unit](OptionDefKind.Cmd, name))

  /** adds final check. */
  def checkConfig(f: C => Either[String, Unit]): OParser[Unit, C] =
    wrap(makeDef[Unit](OptionDefKind.Check, "").validateConfig(f))

  /** adds an option invoked by `--name` that displays header text and exits.
   * @param name name of the option
   */
  def version(name: String): OParser[Unit, C] =
    wrap(makeDef[Unit](OptionDefKind.OptVersion, name))

  /** adds an option invoked by `-x` or `--name` that displays header text and exits.
   * @param x name of the short option
   * @param name name of the option
   */
  def version(x: Char, name: String): OParser[Unit, C] =
    version(name).abbr(x.toString)

  /** adds an option invoked by `--name` that displays usage text and exits.
   * @param name name of the option
   */
  def help(name: String): OParser[Unit, C] =
    wrap(makeDef[Unit](OptionDefKind.OptHelp, name))

  /** adds an option invoked by `-x` or `--name` that displays usage text and exits.
   * @param x name of the short option
   * @param name name of the option
   */
  def help(x: Char, name: String): OParser[Unit, C] =
    help(name).abbr(x.toString)

  /** call this to express success in custom validation. */
  def success: Either[String, Unit] = OptionDef.makeSuccess[String]

  /** call this to express failure in custom validation. */
  def failure(msg: String): Either[String, Unit] = Left(msg)

  protected def wrap[A](d: OptionDef[A, C]): OParser[A, C] =
    OParser(d, Nil)

  protected def makeDef[A: Read](kind: OptionDefKind, name: String): OptionDef[A, C] =
    new OptionDef[A, C](kind, name)
}

/**
 * A monadic commandline options parser.
 * {{{
 * import scopt.OParser
 * val builder = OParser.builder[Config]
 * val parser1 = {
 *   import builder._
 *   OParser.sequence(
 *     programName("scopt"),
 *     head("scopt", "4.x"),
 *     opt[Int]('f', "foo")
 *       .action((x, c) => c.copy(foo = x))
 *       .text("foo is an integer property"),
 *     opt[File]('o', "out")
 *       .required()
 *       .valueName("<file>")
 *       .action((x, c) => c.copy(out = x))
 *       .text("out is a required file property"),
 *     opt[(String, Int)]("max")
 *       .action({ case ((k, v), c) => c.copy(libName = k, maxCount = v) })
 *       .validate(x =>
 *         if (x._2 > 0) success
 *         else failure("Value <max> must be >0"))
 *       .keyValueName("<libname>", "<max>")
 *       .text("maximum count for <libname>"),
 *     opt[Seq[File]]('j', "jars")
 *       .valueName("<jar1>,<jar2>...")
 *       .action((x, c) => c.copy(jars = x))
 *       .text("jars to include"),
 *     opt[Map[String, String]]("kwargs")
 *       .valueName("k1=v1,k2=v2...")
 *       .action((x, c) => c.copy(kwargs = x))
 *       .text("other arguments"),
 *     opt[Unit]("verbose")
 *       .action((_, c) => c.copy(verbose = true))
 *       .text("verbose is a flag"),
 *     opt[Unit]("debug")
 *       .hidden()
 *       .action((_, c) => c.copy(debug = true))
 *       .text("this option is hidden in the usage text"),
 *     help("help").text("prints this usage text"),
 *     arg[File]("<file>...")
 *       .unbounded()
 *       .optional()
 *       .action((x, c) => c.copy(files = c.files :+ x))
 *       .text("optional unbounded args"),
 *     note("some notes." + sys.props("line.separator")),
 *     cmd("update")
 *       .action((_, c) => c.copy(mode = "update"))
 *       .text("update is a command.")
 *       .children(
 *         opt[Unit]("not-keepalive")
 *           .abbr("nk")
 *           .action((_, c) => c.copy(keepalive = false))
 *           .text("disable keepalive"),
 *         opt[Boolean]("xyz")
 *           .action((x, c) => c.copy(xyz = x))
 *           .text("xyz is a boolean property"),
 *         opt[Unit]("debug-update")
 *           .hidden()
 *           .action((_, c) => c.copy(debug = true))
 *           .text("this option is hidden in the usage text"),
 *         checkConfig(
 *           c =>
 *             if (c.keepalive && c.xyz) failure("xyz cannot keep alive")
 *             else success)
 *       )
 *   )
 * }
 *
 * // OParser.parse returns Option[Config]
 * OParser.parse(parser1, args, Config()) match {
 *   case Some(config) =>
 *     // do something
 *   case _ =>
 *     // arguments are bad, error message will have been displayed
 * }
 *
 * // alternatively, use OParser.runParser returns (Option[Config], List[OEffect])
 * OParser.runParser(parser1, args, Config()) match {
 *   case (result, effects) =>
 *     OParser.runEffects(effects, new DefaultOEffectSetup {
 *       // override def displayToOut(msg: String): Unit = Console.out.println(msg)
 *       // override def displayToErr(msg: String): Unit = Console.err.println(msg)
 *       // override def reportError(msg: String): Unit = displayToErr("Error: " + msg)
 *       // override def reportWarning(msg: String): Unit = displayToErr("Warning: " + msg)
 *
 *       // ignore terminate
 *       override def terminate(exitState: Either[String, Unit]): Unit = ()
 *     })
 *
 *     result match {
 *       Some(config) =>
 *         // do something
 *       case _ =>
 *         // arguments are bad, error message will have been displayed
 *     }
 * }
 *}}}
 */
class OParser[A, C](head: OptionDef[A, C], rest: List[OptionDef[_, C]]) {

  /** Adds description in the usage text. */
  def text(x: String): OParser[A, C] = subHead[A](head.text(x))

  /** Adds short option -x. */
  def abbr(x: String): OParser[A, C] = subHead[A](head.abbr(x))

  /** Adds a callback function. */
  def action(f: (A, C) => C): OParser[A, C] = subHead[A](head.action(f))

  /** Requires the option to appear at least `n` times. */
  def minOccurs(n: Int): OParser[A, C] = subHead[A](head.minOccurs(n))

  /** Allows the argument to appear at most `n` times. */
  def maxOccurs(n: Int): OParser[A, C] = subHead[A](head.maxOccurs(n))

  /** Requires the option to appear at least once. */
  def required(): OParser[A, C] = minOccurs(1)

  /** Changes the option to be optional. */
  def optional(): OParser[A, C] = minOccurs(0)

  /** Allows the argument to appear multiple times. */
  def unbounded(): OParser[A, C] = maxOccurs(OptionDef.UNBOUNDED)

  /** Hides the option in any usage text. */
  def hidden(): OParser[A, C] = subHead[A](head.hidden())

  /** Adds value name used in the usage text. */
  def valueName(x: String): OParser[A, C] =
    subHead[A](head.valueName(x))

  /** Adds key name used in the usage text. */
  def keyName(x: String): OParser[A, C] =
    subHead[A](head.keyName(x))

  /** Adds key and value names used in the usage text. */
  def keyValueName(k: String, v: String): OParser[A, C] =
    subHead[A](head.keyValueName(k, v))

  /** Adds a parser under this command. */
  def children(cs: OParser[_, C]*): OParser[A, C] = {
    val options = cs.toList.flatMap(_.toList)
    val (withParent, withoutParent) = options.partition(_.hasParent)
    val updatedChildList = withParent ::: withoutParent.map(_.parent(head))
    OParser(head, rest ::: updatedChildList)
  }

  /** Adds custom validation. */
  def validate(f: A => Either[String, Unit]): OParser[A, C] =
    subHead[A](head.validate(f))

  def toList: List[OptionDef[_, C]] = head :: rest
  def ++(other: OParser[_, C]): OParser[A, C] =
    OParser(head, rest ::: other.toList)

  def foreach(f: Unit => Unit): Unit = f(())

  def map(f: Unit => Unit): OParser[A, C] = this

  def flatMap(f: Unit => OParser[_, C]): OParser[A, C] =
    OParser(head, rest ::: f(()).toList)

  protected def subHead[B](head: OptionDef[B, C]): OParser[B, C] =
    OParser(head, rest)
}

object OParser {
  def apply[A, C](head: OptionDef[A, C], rest: List[OptionDef[_, C]]): OParser[A, C] =
    new OParser(head, rest)

  def builder[C]: OParserBuilder[C] = new OParserBuilder[C] {}

  def usage[C](parser: OParser[_, C]): String = usage(parser, RenderingMode.TwoColumns)

  def usage[C](parser: OParser[_, C], mode: RenderingMode): String = {
    val (h, u) = ORunner.renderUsage[C](mode, parser.toList)
    u
  }

  def sequence[A, C](parser: OParser[A, C], parsers: OParser[_, C]*): OParser[A, C] =
    if (parsers.isEmpty) parser
    else
      parser flatMap { p =>
        val ps = parsers.toList
        sequence(ps.head, ps.tail: _*)
      }

  private[this] lazy val psetup0 = new DefaultOParserSetup with OParserSetup {
    def showUsageAsError(): Unit = ()
    def showTryHelp(): Unit = ()
  }
  private[this] lazy val esetup0 = new DefaultOEffectSetup with OEffectSetup {
    def showUsageAsError(): Unit = ()
    def showTryHelp(): Unit = ()
  }

  /** Run the parser, and run the effects.
   */
  def parse[C](parser: OParser[_, C], args: CSeq[String], init: C): Option[C] =
    ORunner.runParser(args, init, parser.toList, psetup0) match {
      case (r, es) => ORunner.runEffects(es, esetup0); r
    }

  /** Run the parser, and run the effects.
   */
  def parse[C](
      parser: OParser[_, C],
      args: CSeq[String],
      init: C,
      psetup: OParserSetup,
      esetup: OEffectSetup
  ): Option[C] =
    ORunner.runParser(args, init, parser.toList, psetup) match {
      case (r, es) => ORunner.runEffects(es, esetup); r
    }

  /** Run the parser, and run the effects.
   */
  def parse[C](
      parser: OParser[_, C],
      args: CSeq[String],
      init: C,
      psetup: OParserSetup
  ): Option[C] =
    ORunner.runParser(args, init, parser.toList, psetup) match {
      case (r, es) => ORunner.runEffects(es, esetup0); r
    }

  /** Run the parser, and run the effects.
   */
  def parse[C](
      parser: OParser[_, C],
      args: CSeq[String],
      init: C,
      esetup: OEffectSetup
  ): Option[C] =
    ORunner.runParser(args, init, parser.toList, psetup0) match {
      case (r, es) => ORunner.runEffects(es, esetup); r
    }

  /** Run the parser, and return the result and the effects.
   */
  def runParser[C](parser: OParser[_, C], args: CSeq[String], init: C): (Option[C], List[OEffect]) =
    ORunner.runParser(args, init, parser.toList, psetup0)

  /** Run the parser, and return the result and the effects.
   */
  def runParser[C](
      parser: OParser[_, C],
      args: CSeq[String],
      init: C,
      psetup: OParserSetup
  ): (Option[C], List[OEffect]) =
    ORunner.runParser(args, init, parser.toList, psetup)

  def runEffects(es: List[OEffect]): Unit = ORunner.runEffects(es, esetup0)

  def runEffects(es: List[OEffect], esetup: OEffectSetup): Unit = ORunner.runEffects(es, esetup)
}
