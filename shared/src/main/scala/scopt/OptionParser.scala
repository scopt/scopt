package scopt

import collection.mutable.ListBuffer
import scala.collection.{ Seq => CSeq }

/** <code>scopt.immutable.OptionParser</code> is instantiated within your object,
 * set up by an (ordered) sequence of invocations of
 * the various builder methods such as
 * <a href="#opt[A](Char,String)(Read[A]):OptionDef[A,C]"><code>opt</code></a> method or
 * <a href="#arg[A](String)(Read[A]):OptionDef[A,C]"><code>arg</code></a> method.
 * {{{
 * val parser = new scopt.OptionParser[Config]("scopt") {
 *   head("scopt", "3.x")
 *
 *   opt[Int]('f', "foo").action( (x, c) =>
 *     c.copy(foo = x) ).text("foo is an integer property")
 *
 *   opt[File]('o', "out").required().valueName("<file>").
 *     action( (x, c) => c.copy(out = x) ).
 *     text("out is a required file property")
 *
 *   opt[(String, Int)]("max").action({
 *       case ((k, v), c) => c.copy(libName = k, maxCount = v) }).
 *     validate( x =>
 *       if (x._2 > 0) success
 *       else failure("Value <max> must be >0") ).
 *     keyValueName("<libname>", "<max>").
 *     text("maximum count for <libname>")
 *
 *   opt[Seq[File]]('j', "jars").valueName("<jar1>,<jar2>...").action( (x,c) =>
 *     c.copy(jars = x) ).text("jars to include")
 *
 *   opt[Map[String,String]]("kwargs").valueName("k1=v1,k2=v2...").action( (x, c) =>
 *     c.copy(kwargs = x) ).text("other arguments")
 *
 *   opt[Unit]("verbose").action( (_, c) =>
 *     c.copy(verbose = true) ).text("verbose is a flag")
 *
 *   opt[Unit]("debug").hidden().action( (_, c) =>
 *     c.copy(debug = true) ).text("this option is hidden in the usage text")
 *
 *   help("help").text("prints this usage text")
 *
 *   arg[File]("<file>...").unbounded().optional().action( (x, c) =>
 *     c.copy(files = c.files :+ x) ).text("optional unbounded args")
 *
 *   note("some notes.".newline)
 *
 *   cmd("update").action( (_, c) => c.copy(mode = "update") ).
 *     text("update is a command.").
 *     children(
 *       opt[Unit]("not-keepalive").abbr("nk").action( (_, c) =>
 *         c.copy(keepalive = false) ).text("disable keepalive"),
 *       opt[Boolean]("xyz").action( (x, c) =>
 *         c.copy(xyz = x) ).text("xyz is a boolean property"),
 *       opt[Unit]("debug-update").hidden().action( (_, c) =>
 *         c.copy(debug = true) ).text("this option is hidden in the usage text"),
 *       checkConfig( c =>
 *         if (c.keepalive && c.xyz) failure("xyz cannot keep alive")
 *         else success )
 *     )
 * }
 *
 * // parser.parse returns Option[C]
 * parser.parse(args, Config()) match {
 *   case Some(config) =>
 *     // do stuff
 *
 *   case None =>
 *     // arguments are bad, error message will have been displayed
 * }
 * }}}
 */
abstract class OptionParser[C](programName: String) extends OptionDefCallback[C] { self =>
  protected val options = new ListBuffer[OptionDef[_, C]]

  import platform._
  private[scopt] val defaultConfig: DefaultOParserSetup = new DefaultOParserSetup {}
  private[scopt] lazy val (header0, usage0) =
    ORunner.renderUsage(renderingMode, optionsWithProgramName)

  def errorOnUnknownArgument: Boolean = defaultConfig.errorOnUnknownArgument
  def showUsageOnError: Option[Boolean] = defaultConfig.showUsageOnError
  def reportError(msg: String): Unit = defaultConfig.reportError(msg)
  def reportWarning(msg: String): Unit = defaultConfig.reportWarning(msg)
  def renderingMode: RenderingMode = defaultConfig.renderingMode
  def terminate(exitState: Either[String, Unit]): Unit =
    defaultConfig.terminate(exitState)

  /** adds usage text. */
  def head(xs: String*): OptionDef[Unit, C] =
    makeDef[Unit](OptionDefKind.Head, "") text (xs.mkString(" "))

  /** adds an option invoked by `--name x`.
   * @param name name of the option
   */
  def opt[A: Read](name: String): OptionDef[A, C] = makeDef(OptionDefKind.Opt, name)

  /** adds an option invoked by `-x value` or `--name value`.
   * @param x name of the short option
   * @param name name of the option
   */
  def opt[A: Read](x: Char, name: String): OptionDef[A, C] =
    opt[A](name) abbr (x.toString)

  /** adds usage text. */
  def note(x: String): OptionDef[Unit, C] = makeDef[Unit](OptionDefKind.Note, "") text (x)

  /** adds an argument invoked by an option without `-` or `--`.
   * @param name name in the usage text
   */
  def arg[A: Read](name: String): OptionDef[A, C] = makeDef(OptionDefKind.Arg, name).required()

  /** adds a command invoked by an option without `-` or `--`.
   * @param name name of the command
   */
  def cmd(name: String): OptionDef[Unit, C] = makeDef[Unit](OptionDefKind.Cmd, name)

  /** adds an option invoked by `--name` that displays usage text and exits.
   * @param name name of the option
   */
  def help(name: String): OptionDef[Unit, C] = makeDef[Unit](OptionDefKind.OptHelp, name)

  /** adds an option invoked by `-x` or `--name` that displays usage text and exits.
   * @param x name of the short option
   * @param name name of the option
   */
  def help(x: Char, name: String): OptionDef[Unit, C] =
    help(name).abbr(x.toString)

  /** adds an option invoked by `--name` that displays header text and exits.
   * @param name name of the option
   */
  def version(name: String): OptionDef[Unit, C] = makeDef[Unit](OptionDefKind.OptVersion, name)

  /** adds an option invoked by `-x` or `--name` that displays header text and exits.
   * @param x name of the short option
   * @param name name of the option
   */
  def version(x: Char, name: String): OptionDef[Unit, C] =
    version(name).abbr(x.toString)

  /** adds final check. */
  def checkConfig(f: C => Either[String, Unit]): OptionDef[Unit, C] =
    makeDef[Unit](OptionDefKind.Check, "") validateConfig (f)

  def displayToOut(msg: String): Unit = defaultConfig.displayToOut(msg)
  def displayToErr(msg: String): Unit = defaultConfig.displayToErr(msg)
  def displayToWarn(msg: String): Unit = defaultConfig.displayToWarn(msg)

  def header: String = header0
  def usage: String = usage0

  /** call this to express success in custom validation. */
  def success: Either[String, Unit] = OptionDef.makeSuccess[String]

  /** call this to express failure in custom validation. */
  def failure(msg: String): Either[String, Unit] = Left(msg)

  protected def makeDef[A: Read](kind: OptionDefKind, name: String): OptionDef[A, C] =
    updateOption(new OptionDef[A, C](defCallback = this, kind = kind, name = name))
  private[scopt] def onChange[A: Read](option: OptionDef[A, C]): Unit = updateOption(option)
  private[scopt] def updateOption[A: Read](option: OptionDef[A, C]): OptionDef[A, C] = {
    val idx = options indexWhere { _.id == option.id }
    if (idx > -1) options(idx) = option
    else options += option
    option
  }

  /** parses the given `args`.
   * @return `true` if successful, `false` otherwise
   */
  def parse(args: CSeq[String])(implicit ev: Zero[C]): Boolean =
    parse(args, ev.zero) match {
      case Some(x) => true
      case None    => false
    }

  private[scopt] def optionsWithProgramName =
    (new OptionDef[Unit, C](OptionDefKind.ProgramName, "").text(programName)) :: options.toList

  /** parses the given `args`.
   */
  def parse(args: CSeq[String], init: C): Option[C] =
    ORunner.parse(
      args,
      init,
      optionsWithProgramName,
      new OParserSetup {
        override def renderingMode: RenderingMode = self.renderingMode
        override def errorOnUnknownArgument: Boolean = self.errorOnUnknownArgument
        override def showUsageOnError: Option[Boolean] = self.showUsageOnError
        override def displayToOut(msg: String): Unit = self.displayToOut(msg)
        override def displayToErr(msg: String): Unit = self.displayToErr(msg)
        override def displayToWarn(msg: String): Unit = self.displayToWarn(msg)
        override def reportError(msg: String): Unit = self.reportError(msg)
        override def reportWarning(msg: String): Unit = self.reportWarning(msg)

        override def terminate(exitState: Either[String, Unit]): Unit = self.terminate(exitState)
      }
    )
}
