package scopt



import collection.mutable.{ListBuffer, ListMap}

trait Read[A] { self =>
  def arity: Int
  def tokensToRead: Int = if (arity == 0) 0 else 1
  def reads: String => A
  def map[B](f: A => B): Read[B] = new Read[B] {
    val arity = self.arity
    val reads = self.reads andThen f
  }
}

object Read extends platform.PlatformReadInstances {

  import scala.concurrent.duration.Duration

  def reads[A](f: String => A): Read[A] = new Read[A] {
    val arity = 1
    val reads = f
  }
  implicit val stringRead: Read[String]       = reads { identity }
  implicit val charRead: Read[Char]           =
    reads { _.getBytes match {
      case Array(char) => char.toChar
      case s           =>
        throw new IllegalArgumentException("'" + s + "' is not a char.")
    }}
  implicit val doubleRead: Read[Double]       = reads { _.toDouble }
  implicit val booleanRead: Read[Boolean]     =
    reads { _.toLowerCase match {
      case "true"  => true
      case "false" => false
      case "yes"   => true
      case "no"    => false
      case "1"     => true
      case "0"     => false
      case s       =>
        throw new IllegalArgumentException("'" + s + "' is not a boolean.")
    }}

  private def fixedPointWithRadix(str: String): (String, Int) = str.toLowerCase match {
    case s if s.startsWith("0x") => (s.stripPrefix("0x"), 16)
    case s                       => (s, 10)
  }
  implicit val intRead: Read[Int] = reads { str =>
    val (s, radix) = fixedPointWithRadix(str)
    Integer.parseInt(s, radix)
  }
  implicit val longRead: Read[Long] = reads { str =>
    val (s, radix) = fixedPointWithRadix(str)
    java.lang.Long.parseLong(s, radix)
  }
  implicit val bigIntRead: Read[BigInt] = reads { str =>
    val (s, radix) = fixedPointWithRadix(str)
    BigInt(s, radix)
  }

  implicit val bigDecimalRead: Read[BigDecimal] = reads { BigDecimal(_) }

  implicit val durationRead: Read[Duration]   =
    reads { try {
      Duration(_)
    } catch {
      case e: NumberFormatException => throw platform.mkParseEx(e.getMessage, -1)
    }}

  implicit def tupleRead[A1: Read, A2: Read]: Read[(A1, A2)] = new Read[(A1, A2)] {
    val arity = 2
    val reads = { (s: String) =>
      splitKeyValue(s) match {
        case (k, v) => implicitly[Read[A1]].reads(k) -> implicitly[Read[A2]].reads(v)
      }
    }
  }
  private def splitKeyValue(s: String): (String, String) =
    s.indexOf('=') match {
      case -1     => throw new IllegalArgumentException("Expected a key=value pair")
      case n: Int => (s.slice(0, n), s.slice(n + 1, s.length))
    }
  implicit val unitRead: Read[Unit] = new Read[Unit] {
    val arity = 0
    val reads = { (s: String) => () }
  }

  val sep = ","

  // reads("1,2,3,4,5") == Seq(1,2,3,4,5)
  implicit def seqRead[A: Read]: Read[Seq[A]] = reads { (s: String) =>
    s.split(sep).map(implicitly[Read[A]].reads)
  }

  // reads("1=false,2=true") == Map(1 -> false, 2 -> true)
  implicit def mapRead[K: Read, V: Read]: Read[Map[K,V]] = reads { (s: String) =>
    s.split(sep).map(implicitly[Read[(K,V)]].reads).toMap
  }

  // reads("1=false,1=true") == List((1 -> false), (1 -> true))
  implicit def seqTupleRead[K: Read, V: Read]: Read[Seq[(K,V)]] = reads { (s: String) =>
    s.split(sep).map(implicitly[Read[(K,V)]].reads).toSeq
  }
}

trait Zero[A] {
  def zero: A
}
object Zero {
  def zero[A](f: => A): Zero[A] = new Zero[A] {
    val zero = f
  }
  implicit val intZero: Zero[Int]             = zero(0)
  implicit val unitZero: Zero[Unit]           = zero(())
}

object Validation {
  def validateValue[A](vs: Seq[A => Either[String, Unit]])(value: A): Either[Seq[String], Unit] = {
    val results = vs map {_.apply(value)}
    (OptionDef.makeSuccess[Seq[String]] /: results) { (acc, r) =>
      (acc match {
        case Right(_) => Seq[String]()
        case Left(xs) => xs
      }) ++ (r match {
        case Right(_) => Seq[String]()
        case Left(x)  => Seq[String](x)
      }) match {
        case Seq()    => acc
        case xs       => Left(xs)
      }
    }
  }
}

trait RenderingMode
object RenderingMode {
  case object OneColumn extends RenderingMode
  case object TwoColumns extends RenderingMode
}

private[scopt] sealed trait OptionDefKind {}
private[scopt] case object Opt extends OptionDefKind
private[scopt] case object Note extends OptionDefKind
private[scopt] case object Arg extends OptionDefKind
private[scopt] case object Cmd extends OptionDefKind
private[scopt] case object Head extends OptionDefKind
private[scopt] case object Check extends OptionDefKind

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
abstract class OptionParser[C](programName: String) {
  protected val options = new ListBuffer[OptionDef[_, C]]
  protected val helpOptions = new ListBuffer[OptionDef[_, C]]

  import platform._

  def errorOnUnknownArgument: Boolean = true
  def showUsageOnError: Boolean = helpOptions.isEmpty
  def renderingMode: RenderingMode = RenderingMode.TwoColumns
  def terminate(exitState: Either[String, Unit]): Unit =
    exitState match {
      case Left(_)  => sys.exit(1)
      case Right(_) => sys.exit(0)
    }

  def reportError(msg: String): Unit = {
    Console.err.println("Error: " + msg)
  }

  def reportWarning(msg: String): Unit = {
    Console.err.println("Warning: " + msg)
  }

  def showTryHelp(): Unit = {
    def oxford(xs: List[String]): String = xs match {
      case a :: b :: Nil => a + " or " + b
      case _             => (xs.dropRight(2) :+ xs.takeRight(2).mkString(", or ")).mkString(", ")
    }
    Console.err.println("Try " + oxford(helpOptions.toList map {_.fullName}) + " for more information.")
  }

  /** adds usage text. */
  def head(xs: String*): OptionDef[Unit, C] = makeDef[Unit](Head, "") text(xs.mkString(" "))

  /** adds an option invoked by `--name x`.
   * @param name name of the option
   */
  def opt[A: Read](name: String): OptionDef[A, C] = makeDef(Opt, name)

  /** adds an option invoked by `-x value` or `--name value`.
   * @param x name of the short option
   * @param name name of the option
   */
  def opt[A: Read](x: Char, name: String): OptionDef[A, C] =
    opt[A](name) abbr(x.toString)

  /** adds usage text. */
  def note(x: String): OptionDef[Unit, C] = makeDef[Unit](Note, "") text(x)

  /** adds an argument invoked by an option without `-` or `--`.
   * @param name name in the usage text
   */
  def arg[A: Read](name: String): OptionDef[A, C] = makeDef(Arg, name) required()

  /** adds a command invoked by an option without `-` or `--`.
   * @param name name of the command
   */
  def cmd(name: String): OptionDef[Unit, C] = makeDef[Unit](Cmd, name)

  /** adds an option invoked by `--name` that displays usage text and exits.
   * @param name name of the option
   */
  def help(name: String): OptionDef[Unit, C] = {
    val o = opt[Unit](name) action { (x, c) =>
      showUsage()
      terminate(Right(()))
      c
    }
    helpOptions += o
    o
  }


  /** adds an option invoked by `--name` that displays header text and exits.
   * @param name name of the option
   */
  def version(name: String): OptionDef[Unit, C] =
    opt[Unit](name) action { (x, c) =>
      showHeader()
      terminate(Right(()))
      c
    }

  /** adds final check. */
  def checkConfig(f: C => Either[String, Unit]): OptionDef[Unit, C] =
    makeDef[Unit](Check, "") validateConfig(f)

  def showHeader() {
    Console.out.println(header)
  }
  def header: String = {
    import OptionDef._
    (heads map {_.usage}).mkString(NL)
  }

  def showUsage(): Unit = {
    Console.out.println(usage)
  }
  def showUsageAsError(): Unit = {
    Console.err.println(usage)
  }
  def usage: String = renderUsage(renderingMode)
  def renderUsage(mode: RenderingMode): String =
    mode match {
      case RenderingMode.OneColumn => renderOneColumnUsage
      case RenderingMode.TwoColumns => renderTwoColumnsUsage
    }
  def renderOneColumnUsage: String = {
    import OptionDef._
    val descriptions = optionsForRender map {_.usage}
    (if (header == "") "" else header + NL) +
    "Usage: " + usageExample + NLNL +
    descriptions.mkString(NL)
  }
  def renderTwoColumnsUsage: String = {
    import OptionDef._
    val xs = optionsForRender
    val descriptions = {
      val col1Len = math.min(column1MaxLength, xs map {_.usageColumn1.length + WW.length} match {
        case Nil => 0
        case list => list.max
      })
      xs map {_.usageTwoColumn(col1Len)}
    }
    (if (header == "") "" else header + NL) +
    "Usage: " + usageExample + NLNL +
    descriptions.mkString(NL)
  }
  def optionsForRender: List[OptionDef[_, C]] = {
    val unsorted = options filter { o => o.kind != Head && o.kind != Check && !o.isHidden }
    val (remaining, sorted) = unsorted partition {_.hasParent} match {
      case (p, np) => (ListBuffer() ++ p, ListBuffer() ++ np)
    }
    var continue = true
    while (!remaining.isEmpty && continue) {
      continue = false
      for {
        parent <- sorted
      } {
        val childrenOfThisParent = remaining filter {_.getParentId == Some(parent.id)}
        if (childrenOfThisParent.nonEmpty) {
          remaining --= childrenOfThisParent
          sorted.insertAll((sorted indexOf parent) + 1, childrenOfThisParent)
          continue = true
        }
      }
    }
    sorted.toList
  }
  def usageExample: String = commandExample(None)
  private[scopt] def commandExample(cmd: Option[OptionDef[_, C]]): String = {
    val text = new ListBuffer[String]()
    text += cmd map {commandName} getOrElse programName
    val parentId = cmd map {_.id}
    val cs = commands filter {_.getParentId == parentId}
    if (cs.nonEmpty) text += cs map {_.name} mkString("[", "|", "]")
    val os = options.toSeq filter { case x => x.kind == Opt && x.getParentId == parentId }
    val as = arguments filter {_.getParentId == parentId}
    if (os.nonEmpty) text += "[options]"
    if (cs exists { case x => arguments exists {_.getParentId == Some(x.id)}}) text += "<args>..."
    else if (as.nonEmpty) text ++= as map {_.argName}
    text.mkString(" ")
  }
  private[scopt] def commandName(cmd: OptionDef[_, C]): String =
    (cmd.getParentId map { x =>
      (commands find {_.id == x} map {commandName} getOrElse {""}) + " "
    } getOrElse {""}) + cmd.name

  /** call this to express success in custom validation. */
  def success: Either[String, Unit] = OptionDef.makeSuccess[String]
  /** call this to express failure in custom validation. */
  def failure(msg: String): Either[String, Unit] = Left(msg)

  protected def heads: Seq[OptionDef[_, C]] = options.toSeq filter {_.kind == Head}
  protected def nonArgs: Seq[OptionDef[_, C]] = options.toSeq filter { case x => x.kind == Opt || x.kind == Note }
  protected def arguments: Seq[OptionDef[_, C]] = options.toSeq filter {_.kind == Arg}
  protected def commands: Seq[OptionDef[_, C]] = options.toSeq filter {_.kind == Cmd}
  protected def checks: Seq[OptionDef[_, C]] = options.toSeq filter {_.kind == Check}
  protected def makeDef[A: Read](kind: OptionDefKind, name: String): OptionDef[A, C] =
    updateOption(new OptionDef[A, C](parser = this, kind = kind, name = name))
  private[scopt] def updateOption[A: Read](option: OptionDef[A, C]): OptionDef[A, C] = {
    val idx = options indexWhere { _.id == option.id }
    if (idx > -1) options(idx) = option
    else options += option
    option
  }

  /** parses the given `args`.
   * @return `true` if successful, `false` otherwise
   */
  def parse(args: Seq[String])(implicit ev: Zero[C]): Boolean =
    parse(args, ev.zero) match {
      case Some(x) => true
      case None    => false
    }

  /** parses the given `args`.
   */
  def parse(args: Seq[String], init: C): Option[C] = {
    var i = 0
    val pendingOptions = ListBuffer() ++ (nonArgs filterNot {_.hasParent})
    val pendingArgs = ListBuffer() ++ (arguments filterNot {_.hasParent})
    val pendingCommands = ListBuffer() ++ (commands filterNot {_.hasParent})
    val occurrences = ListMap[OptionDef[_, C], Int]().withDefaultValue(0)
    var _config: C = init
    var _error = false

    def pushChildren(opt: OptionDef[_, C]): Unit = {
      // commands are cleared to guarantee that it appears first
      pendingCommands.clear()

      pendingOptions insertAll (0, nonArgs filter { x => x.getParentId == Some(opt.id) &&
        !pendingOptions.contains(x) })
      pendingArgs insertAll (0, arguments filter { x => x.getParentId == Some(opt.id) &&
        !pendingArgs.contains(x) })
      pendingCommands insertAll (0, commands filter { x => x.getParentId == Some(opt.id) &&
        !pendingCommands.contains(x) })
    }
    def handleError(msg: String): Unit = {
      if (errorOnUnknownArgument) {
        _error = true
        reportError(msg)
      }
      else reportWarning(msg)
    }
    def handleArgument(opt: OptionDef[_, C], arg: String): Unit = {
      opt.applyArgument(arg, _config) match {
        case Right(c) =>
          _config = c
          pushChildren(opt)
        case Left(xs) =>
          _error = true
          xs foreach reportError
      }
    }
    def handleOccurrence(opt: OptionDef[_, C], pending: ListBuffer[OptionDef[_, C]]): Unit = {
      occurrences(opt) += 1
      if (occurrences(opt) >= opt.getMaxOccurs) {
        pending -= opt
      }
    }
    def findCommand(cmd: String): Option[OptionDef[_, C]] =
      pendingCommands find {_.name == cmd}
    // greedy match
    def handleShortOptions(g0: String): Unit = {
      val gs =  (0 to g0.size - 1).toSeq map { n => g0.substring(0, g0.size - n) }
      gs flatMap { g => pendingOptions map {(g, _)} } find { case (g, opt) =>
        opt.shortOptTokens("-" + g) == 1
      } match {
        case Some(p) =>
          val (g, option) = p
          handleOccurrence(option, pendingOptions)
          handleArgument(option, "")
          if (g0.drop(g.size) != "") {
            handleShortOptions(g0 drop g.size)
          }
        case None => handleError("Unknown option " + "-" + g0)
      }
    }
    def handleChecks(c: C): Unit = {
      Validation.validateValue(checks flatMap {_.checks})(c) match {
        case Right(c) => // do nothing
        case Left(xs) =>
          _error = true
          xs foreach reportError
      }
    }
    while (i < args.length) {
      pendingOptions find {_.tokensToRead(i, args) > 0} match {
        case Some(option) =>
          handleOccurrence(option, pendingOptions)
          option(i, args) match {
            case Right(v) =>          handleArgument(option, v)
            case Left(outOfBounds) => handleError(outOfBounds)
          }
          // move index forward for gobbling
          if (option.tokensToRead(i, args) > 1) {
            i += option.tokensToRead(i, args) - 1
          } // if
        case None =>
          args(i) match {
            case arg if arg startsWith "--" => handleError("Unknown option " + arg)
            case arg if arg startsWith "-"  =>
              if (arg == "-") handleError("Unknown option " + arg)
              else handleShortOptions(arg drop 1)
            case arg if findCommand(arg).isDefined =>
              val cmd = findCommand(arg).get
              handleOccurrence(cmd, pendingCommands)
              handleArgument(cmd, "")
            case arg if pendingArgs.isEmpty => handleError("Unknown argument '" + arg + "'")
            case arg =>
              val first = pendingArgs.head
              handleOccurrence(first, pendingArgs)
              handleArgument(first, arg)
          }
      }
      i += 1
    }
    (pendingOptions filter { opt => opt.getMinOccurs > occurrences(opt) }) foreach { opt =>
      if (opt.getMinOccurs == 1) reportError("Missing " + opt.shortDescription)
      else reportError(opt.shortDescription.capitalize + " must be given " + opt.getMinOccurs + " times")
      _error = true
    }
    (pendingArgs filter { arg => arg.getMinOccurs > occurrences(arg) }) foreach { arg =>
      if (arg.getMinOccurs == 1) reportError("Missing " + arg.shortDescription)
      else reportError(arg.shortDescription.capitalize + "' must be given " + arg.getMinOccurs + " times")
      _error = true
    }
    handleChecks(_config)
    if (_error) {
      if (showUsageOnError) showUsageAsError()
      else showTryHelp()
      None
    }
    else Some(_config)
  }
}

class OptionDef[A: Read, C](
  _parser: OptionParser[C],
  _id: Int,
  _kind: OptionDefKind,
  _name: String,
  _shortOpt: Option[String],
  _keyName: Option[String],
  _valueName: Option[String],
  _desc: String,
  _action: (A, C) => C,
  _validations: Seq[A => Either[String, Unit]],
  _configValidations: Seq[C => Either[String, Unit]],
  _parentId: Option[Int],
  _minOccurs: Int,
  _maxOccurs: Int,
  _isHidden: Boolean) {

  import platform._
  import OptionDef._

  def this(parser: OptionParser[C], kind: OptionDefKind, name: String) =
    this(_parser = parser, _id = OptionDef.generateId, _kind = kind, _name = name,
      _shortOpt = None, _keyName = None, _valueName = None,
      _desc = "", _action = { (a: A, c: C) => c },
      _validations = Seq(), _configValidations = Seq(),
      _parentId = None, _minOccurs = 0, _maxOccurs = 1,
      _isHidden = false)

  private[scopt] def copy(
    _parser: OptionParser[C] = this._parser,
    _id: Int = this._id,
    _kind: OptionDefKind = this._kind,
    _name: String = this._name,
    _shortOpt: Option[String] = this._shortOpt,
    _keyName: Option[String] = this._keyName,
    _valueName: Option[String] = this._valueName,
    _desc: String = this._desc,
    _action: (A, C) => C = this._action,
    _validations: Seq[A => Either[String, Unit]] = this._validations,
    _configValidations: Seq[C => Either[String, Unit]] = this._configValidations,
    _parentId: Option[Int] = this._parentId,
    _minOccurs: Int = this._minOccurs,
    _maxOccurs: Int = this._maxOccurs,
    _isHidden: Boolean = this._isHidden): OptionDef[A, C] =
    new OptionDef(_parser = _parser, _id = _id, _kind = _kind, _name = _name, _shortOpt = _shortOpt,
      _keyName = _keyName, _valueName = _valueName, _desc = _desc, _action = _action,
      _validations = _validations, _configValidations = _configValidations,
      _parentId = _parentId, _minOccurs = _minOccurs, _maxOccurs = _maxOccurs,
      _isHidden = _isHidden)

  private[this] def read: Read[A] = implicitly[Read[A]]

  /** Adds a callback function. */
  def action(f: (A, C) => C): OptionDef[A, C] =
    _parser.updateOption(copy(_action = (a: A, c: C) => { f(a, _action(a, c)) }))
  /** Adds a callback function. */
  def foreach(f: A => Unit): OptionDef[A, C] =
    _parser.updateOption(copy(_action = (a: A, c: C) => {
      val c2 = _action(a, c)
      f(a)
      c2
    }))

  override def toString: String = fullName

  /** Adds short option -x. */
  def abbr(x: String): OptionDef[A, C] =
    _parser.updateOption(copy(_shortOpt = Some(x)))
  /** Requires the option to appear at least `n` times. */
  def minOccurs(n: Int): OptionDef[A, C] =
    _parser.updateOption(copy(_minOccurs = n))
  /** Requires the option to appear at least once. */
  def required(): OptionDef[A, C] = minOccurs(1)
  /** Chanages the option to be optional. */
  def optional(): OptionDef[A, C] = minOccurs(0)
  /** Allows the argument to appear at most `n` times. */
  def maxOccurs(n: Int): OptionDef[A, C] =
    _parser.updateOption(copy(_maxOccurs = n))
  /** Allows the argument to appear multiple times. */
  def unbounded(): OptionDef[A, C] = maxOccurs(UNBOUNDED)
  /** Adds description in the usage text. */
  def text(x: String): OptionDef[A, C] =
    _parser.updateOption(copy(_desc = x))
  /** Adds value name used in the usage text. */
  def valueName(x: String): OptionDef[A, C] =
    _parser.updateOption(copy(_valueName = Some(x)))
  /** Adds key name used in the usage text. */
  def keyName(x: String): OptionDef[A, C] =
    _parser.updateOption(copy(_keyName = Some(x)))
  /** Adds key and value names used in the usage text. */
  def keyValueName(k: String, v: String): OptionDef[A, C] =
    keyName(k) valueName(v)
  /** Adds custom validation. */
  def validate(f: A => Either[String, Unit]) =
    _parser.updateOption(copy(_validations = _validations :+ f))
  /** Hides the option in any usage text. */
  def hidden(): OptionDef[A, C] =
    _parser.updateOption(copy(_isHidden = true))

  private[scopt] def validateConfig(f: C => Either[String, Unit]) =
    _parser.updateOption(copy(_configValidations = _configValidations :+ f))
  private[scopt] def parent(x: OptionDef[_, C]): OptionDef[A, C] =
    _parser.updateOption(copy(_parentId = Some(x.id)))
  /** Adds opt/arg under this command. */
  def children(xs: OptionDef[_, C]*): OptionDef[A, C] = {
    xs foreach {_.parent(this)}
    this
  }

  private[scopt] val kind: OptionDefKind = _kind
  private[scopt] val id: Int = _id
  private[scopt] val name: String = _name
  private[scopt] def callback: (A, C) => C = _action
  private[scopt] def getMinOccurs: Int = _minOccurs
  private[scopt] def getMaxOccurs: Int = _maxOccurs
  private[scopt] def shortOptOrBlank: String = _shortOpt getOrElse("")
  private[scopt] def hasParent: Boolean = _parentId.isDefined
  private[scopt] def getParentId: Option[Int] = _parentId
  private[scopt] def isHidden: Boolean = _isHidden
  private[scopt] def checks: Seq[C => Either[String, Unit]] = _configValidations

  private[scopt] def applyArgument(arg: String, config: C): Either[Seq[String], C] =
    try {
      val x = read.reads(arg)
      Validation.validateValue(_validations)(x) match {
        case Right(_) => Right(callback(x, config))
        case Left(xs) => Left(xs)
      }
    } catch applyArgumentExHandler(shortDescription.capitalize, arg)

  // number of tokens to read: 0 for no match, 2 for "--foo 1", 1 for "--foo:1"
  private[scopt] def shortOptTokens(arg: String): Int =
    _shortOpt match {
      case Some(c) if arg == "-" + shortOptOrBlank                 => 1 + read.tokensToRead
      case Some(c) if arg startsWith ("-" + shortOptOrBlank + ":") => 1
      case Some(c) if arg startsWith ("-" + shortOptOrBlank + "=") => 1
      case _ => 0
    }
  private[scopt] def longOptTokens(arg: String): Int =
    if (arg == fullName) 1 + read.tokensToRead
    else if ((arg startsWith (fullName + ":")) || (arg startsWith (fullName + "="))) 1
    else 0
  private[scopt] def tokensToRead(i: Int, args: Seq[String]): Int =
    if (i >= args.length || kind != Opt) 0
    else args(i) match {
      case arg if longOptTokens(arg) > 0  => longOptTokens(arg)
      case arg if shortOptTokens(arg) > 0 => shortOptTokens(arg)
      case _ => 0
    }
  private[scopt] def apply(i: Int, args: Seq[String]): Either[String, String] =
    if (i >= args.length || kind != Opt) Left("Option does not match")
    else args(i) match {
      case arg if longOptTokens(arg) == 2 || shortOptTokens(arg) == 2 =>
        token(i + 1, args) map {Right(_)} getOrElse Left("Missing value after " + arg)
      case arg if longOptTokens(arg) == 1 && read.tokensToRead == 1 =>
        Right(arg drop (fullName + ":").length)
      case arg if shortOptTokens(arg) == 1 && read.tokensToRead == 1 =>
        Right(arg drop ("-" + shortOptOrBlank + ":").length)
      case _ => Right("")
    }
  private[scopt] def token(i: Int, args: Seq[String]): Option[String] =
    if (i >= args.length || kind != Opt) None
    else Some(args(i))
  private[scopt] def usage: String =
    kind match {
      case Head | Note | Check => _desc
      case Cmd =>
        "Command: " + _parser.commandExample(Some(this)) +  NL + _desc
      case Arg => WW + name + NLTB + _desc
      case Opt if read.arity == 2 =>
        WW + (_shortOpt map { o => "-" + o + ":" + keyValueString + " | " } getOrElse { "" }) +
        fullName + ":" + keyValueString + NLTB + _desc
      case Opt if read.arity == 1 =>
        WW + (_shortOpt map { o => "-" + o + " " + valueString + " | " } getOrElse { "" }) +
        fullName + " " + valueString + NLTB + _desc
      case Opt =>
        WW + (_shortOpt map { o => "-" + o + " | " } getOrElse { "" }) +
        fullName + NLTB + _desc
    }
  private[scopt] def usageTwoColumn(col1Length: Int): String = {
    def spaceToDesc(str: String) = if (str.length <= col1Length) str + " " * (col1Length - str.length)
                                   else str.dropRight(WW.length) + NL + " " * col1Length
    kind match {
      case Head | Note | Check => _desc
      case Cmd => usageColumn1 + _desc
      case Arg => spaceToDesc(usageColumn1 + WW) + _desc
      case Opt if read.arity == 2 => spaceToDesc(usageColumn1 + WW) + _desc
      case Opt if read.arity == 1 => spaceToDesc(usageColumn1 + WW) + _desc
      case Opt => spaceToDesc(usageColumn1 + WW) + _desc
    }
  }
  private[scopt] def usageColumn1: String =
    kind match {
      case Head | Note | Check => ""
      case Cmd =>
        "Command: " + _parser.commandExample(Some(this)) + NL
      case Arg => WW + name
      case Opt if read.arity == 2 =>
        WW + (_shortOpt map { o => "-" + o + ", " } getOrElse { "" }) +
        fullName + ":" + keyValueString
      case Opt if read.arity == 1 =>
        WW + (_shortOpt map { o => "-" + o + ", " } getOrElse { "" }) +
        fullName + " " + valueString
      case Opt =>
        WW + (_shortOpt map { o => "-" + o + ", " } getOrElse { "" }) +
        fullName
    }
  private[scopt] def keyValueString: String = (_keyName getOrElse defaultKeyName) + "=" + valueString
  private[scopt] def valueString: String = (_valueName getOrElse defaultValueName)
  def shortDescription: String =
    kind match {
      case Opt => "option " + fullName
      case Cmd => "command " + fullName
      case _   => "argument " + fullName
    }
  def fullName: String =
    kind match {
      case Opt => "--" + name
      case _   => name
    }
  private[scopt] def argName: String =
    kind match {
      case Arg if getMinOccurs == 0 => "[" + fullName + "]"
      case _   => fullName
    }
}


private[scopt] object OptionDef {
  val UNBOUNDED = Int.MaxValue
  val NL = platform._NL
  val WW = "  "
  val TB = "        "
  val NLTB = NL + TB
  val NLNL = NL + NL
  val column1MaxLength = 25 + WW.length
  val defaultKeyName = "<key>"
  val defaultValueName = "<value>"
  val atomic = new java.util.concurrent.atomic.AtomicInteger
  def generateId: Int = atomic.incrementAndGet
  def makeSuccess[A]: Either[A, Unit] = Right(())
}
