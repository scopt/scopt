package scopt

import collection.mutable.{ListBuffer, ListMap}

trait Read[A] {
  def arity: Int
  def tokensToRead: Int = if (arity == 0) 0 else 1
  def reads: String => A
}
object Read {
  import java.util.{Locale, Calendar, GregorianCalendar}
  import java.text.SimpleDateFormat
  import java.io.File
  import java.net.URI
  def reads[A](f: String => A): Read[A] = new Read[A] {
    val arity = 1
    val reads = f
  }
  implicit val intRead: Read[Int]             = reads { _.toInt }
  implicit val stringRead: Read[String]       = reads { identity }
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
  implicit val longRead: Read[Long]           = reads { _.toLong }
  implicit val bigIntRead: Read[BigInt]       = reads { BigInt(_) }
  implicit val bigDecimalRead: Read[BigDecimal] = reads { BigDecimal(_) }
  implicit val yyyymmdddRead: Read[Calendar] = calendarRead("yyyy-MM-dd")
  def calendarRead(pattern: String): Read[Calendar] = calendarRead(pattern, Locale.getDefault)
  def calendarRead(pattern: String, locale: Locale): Read[Calendar] =
    reads { s =>
      val fmt = new SimpleDateFormat(pattern)
      val c = new GregorianCalendar
      c.setTime(fmt.parse(s))
      c
    }
  implicit val fileRead: Read[File]           = reads { new File(_) }
  implicit val uriRead: Read[URI]             = reads { new URI(_) }

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

private[scopt] sealed trait OptionDefKind {}
private[scopt] case object Opt extends OptionDefKind
private[scopt] case object Note extends OptionDefKind
private[scopt] case object Arg extends OptionDefKind
private[scopt] case object Cmd extends OptionDefKind
private[scopt] case object Head extends OptionDefKind 

/** <code>scopt.immutable.OptionParser</code> is instantiated within your object,
 * set up by an (ordered) sequence of invocations of 
 * the various builder methods such as
 * <a href="#opt[A](Char,String)(Read[A]):OptionDef[A,C]"><code>opt</code></a> method or
 * <a href="#arg[A](String)(Read[A]):OptionDef[A,C]"><code>arg</code></a> method.
 * {{{
 * val parser = new scopt.OptionParser[Config]("scopt") {
 *   head("scopt", "3.x")
 *   opt[Int]('f', "foo") action { (x, c) =>
 *     c.copy(foo = x) } text("foo is an integer property")
 *   opt[File]('o', "out") required() valueName("<file>") action { (x, c) =>
 *     c.copy(out = x) } text("out is a required file property")
 *   opt[(String, Int)]("max") action { case ((k, v), c) =>
 *     c.copy(libName = k, maxCount = v) } validate { x =>
 *     if (x._2 > 0) success else failure("Value <max> must be >0") 
 *   } keyValueName("<libname>", "<max>") text("maximum count for <libname>")
 *   opt[Unit]("verbose") action { (_, c) =>
 *     c.copy(verbose = true) } text("verbose is a flag")
 *   note("some notes.\n")
 *   help("help") text("prints this usage text")
 *   arg[File]("<file>...") unbounded() optional() action { (x, c) =>
 *     c.copy(files = c.files :+ x) } text("optional unbounded args")
 *   cmd("update") action { (_, c) =>
 *     c.copy(mode = "update") } text("update is a command.") children {
 *     opt[Boolean]("xyz") action { (x, c) =>
 *       c.copy(xyz = x) } text("xyz is a boolean property")
 *   }
 * }
 * // parser.parse returns Option[C]
 * parser.parse(args, Config()) map { config =>
 *   // do stuff
 * } getOrElse {
 *   // arguments are bad, usage message will have been displayed
 * }
 * }}}
 */
abstract case class OptionParser[C](programName: String) {
  import OptionDef._

  def errorOnUnknownArgument: Boolean = true
  
  def reportError(msg: String) {
    System.err.println("Error: " + msg)
  }
  
  def reportWarning(msg: String) {
    System.err.println("Warning: " + msg)
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
    opt[A](name) shortOpt(x)

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
  def help(name: String): OptionDef[Unit, C] =
    opt[Unit](name) action { (x, c) =>
      showUsage
      sys.exit
      c
    }

  /** adds an option invoked by `--name` that displays header text and exits.
   * @param name name of the option
   */
  def version(name: String): OptionDef[Unit, C] =
    opt[Unit](name) action { (x, c) =>
      showHeader
      sys.exit
      c
    }

  def showHeader {
    System.err.println(header)
  }
  def header: String = {
    import OptionDef._
    (heads map {_.usage}).mkString(NL)
  }

  def showUsage {
    System.err.println(usage)
  }
  def usage: String = {
    import OptionDef._
    val unsorted = options filter {_.kind != Head}
    val (unseen, xs) = unsorted partition {_.hasParent} match {
      case (p, np) => (ListBuffer() ++ p, ListBuffer() ++ np)
    }
    while (!unseen.isEmpty) {
      for {
        x <- xs
      } {
        val cs = unseen filter {_.getParentId == Some(x.id)}
        unseen --= cs
        xs.insertAll((xs indexOf x) + 1, cs)
      }
    }
    val descriptions = xs map {_.usage}
    (if (header == "") "" else header + NL) +
    "Usage: " + commandExample(None) + NLNL +
    descriptions.mkString(NL)
  }
  private[scopt] def commandName(cmd: OptionDef[_, C]): String =
    (cmd.getParentId map { x =>
      (commands find {_.id == x} map {commandName} getOrElse {""}) + " "
    } getOrElse {""}) + cmd.name
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

  /** call this to express success in custom validation. */
  def success: Either[String, Unit] = OptionDef.makeSuccess[String]
  /** call this to express failure in custom validation. */
  def failure(msg: String): Either[String, Unit] = Left(msg)

  protected val options = new ListBuffer[OptionDef[_, C]]
  protected def heads: Seq[OptionDef[_, C]] = options.toSeq filter {_.kind == Head}
  protected def nonArgs: Seq[OptionDef[_, C]] = options.toSeq filter { case x => x.kind == Opt || x.kind == Note }
  protected def arguments: Seq[OptionDef[_, C]] = options.toSeq filter {_.kind == Arg}
  protected def commands: Seq[OptionDef[_, C]] = options.toSeq filter {_.kind == Cmd}
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

    def pushChildren(opt: OptionDef[_, C]) {
      // commands are cleared to guarantee that it appears first
      pendingCommands.clear

      pendingOptions insertAll (0, nonArgs filter { x => x.getParentId == Some(opt.id) &&
        !pendingOptions.contains(x) })
      pendingArgs insertAll (0, arguments filter { x => x.getParentId == Some(opt.id) &&
        !pendingArgs.contains(x) })
      pendingCommands insertAll (0, commands filter { x => x.getParentId == Some(opt.id) &&
        !pendingCommands.contains(x) })
    }
    def handleError(msg: String) {
      if (errorOnUnknownArgument) {
        _error = true
        reportError(msg)
      }
      else reportWarning(msg)
    }
    def handleArgument(opt: OptionDef[_, C], arg: String) {
      opt.applyArgument(arg, _config) match {
        case Right(c) =>
          _config = c
          pushChildren(opt)
        case Left(xs) =>
          _error = true
          xs foreach reportError
      }
    }
    def handleOccurrence(opt: OptionDef[_, C], pending: ListBuffer[OptionDef[_, C]]) {
      occurrences(opt) += 1
      if (occurrences(opt) >= opt.getMaxOccurs) {
        pending -= opt
      }
    }
    def findCommand(cmd: String): Option[OptionDef[_, C]] =
      pendingCommands find {_.name == cmd}
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
              val cs = (arg drop 1).toSeq
              if (cs.isEmpty) handleError("Unknown option " + arg)
              else cs foreach { c =>
                pendingOptions find {_.shortOptTokens("-" + c.toString) == 1} match {
                  case Some(option) =>
                    handleOccurrence(option, pendingOptions)
                    handleArgument(option, "")
                  case None => handleError("Unknown option " + "-" + c.toString)
                }
              }
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
    if (_error) {
      showUsage
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
  _shortOpt: Option[Char],
  _keyName: Option[String],
  _valueName: Option[String],
  _desc: String,
  _action: (A, C) => C,
  _validations: Seq[A => Either[String, Unit]],
  _parentId: Option[Int],
  _minOccurs: Int,
  _maxOccurs: Int) {
  import OptionDef._
  
  def this(parser: OptionParser[C], kind: OptionDefKind, name: String) =
    this(_parser = parser, _id = OptionDef.generateId, _kind = kind, _name = name,
      _shortOpt = None, _keyName = None, _valueName = None,
      _desc = "", _action = { (a: A, c: C) => c },
      _validations = Seq(), _parentId = None, _minOccurs = 0, _maxOccurs = 1)

  private[scopt] def copy(
    _parser: OptionParser[C] = this._parser,
    _id: Int = this._id,
    _kind: OptionDefKind = this._kind,
    _name: String = this._name,
    _shortOpt: Option[Char] = this._shortOpt,
    _keyName: Option[String] = this._keyName,
    _valueName: Option[String] = this._valueName,
    _desc: String = this._desc,
    _action: (A, C) => C = this._action,
    _validations: Seq[A => Either[String, Unit]] = this._validations,
    _parentId: Option[Int] = this._parentId,
    _minOccurs: Int = this._minOccurs,
    _maxOccurs: Int = this._maxOccurs): OptionDef[A, C] =
    new OptionDef(_parser = _parser, _id = _id, _kind = _kind, _name = _name, _shortOpt = _shortOpt,
      _keyName = _keyName, _valueName = _valueName, _desc = _desc, _action = _action,
      _validations = _validations, _parentId = _parentId, _minOccurs = _minOccurs, _maxOccurs = _maxOccurs)

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
  def shortOpt(x: Char): OptionDef[A, C] =
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
  private[scopt] def shortOptOrBlank: String = _shortOpt map {_.toString} getOrElse("")
  private[scopt] def hasParent: Boolean = _parentId.isDefined
  private[scopt] def getParentId: Option[Int] = _parentId

  private[scopt] def applyArgument(arg: String, config: C): Either[Seq[String], C] =
    try {
      val x = read.reads(arg)
      validateValue(x) match {
        case Right(_) => Right(callback(x, config))
        case Left(xs) => Left(xs)
      }
    } catch {
      case e: NumberFormatException => Left(Seq(shortDescription.capitalize + " expects a number but was given '" + arg + "'"))
      case e: Throwable             => Left(Seq(shortDescription.capitalize + " failed when given '" + arg + "'. " + e.getMessage))
    }
  private[scopt] def validateValue(value: A): Either[Seq[String], Unit] = {
    val results = _validations map {_.apply(value)}
    (makeSuccess[Seq[String]] /: results) { (acc, r) =>
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
  // number of tokens to read: 0 for no match, 2 for "--foo 1", 1 for "--foo:1"
  private[scopt] def shortOptTokens(arg: String): Int =
    _shortOpt match {
      case Some(c) if arg == "-" + shortOptOrBlank                 => 1 + read.tokensToRead
      case Some(c) if arg startsWith ("-" + shortOptOrBlank + ":") => 1
      case _ => 0
    }
  private[scopt] def longOptTokens(arg: String): Int =
    if (arg == fullName) 1 + read.tokensToRead
    else if (arg startsWith (fullName + ":")) 1
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
      case Head => _desc
      case Note => _desc
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
  val UNBOUNDED = 1024
  val NL = System.getProperty("line.separator")
  val WW = "  "
  val TB = "        "
  val NLTB = NL + TB
  val NLNL = NL + NL
  val defaultKeyName = "<key>"
  val defaultValueName = "<value>"
  val atomic = new java.util.concurrent.atomic.AtomicInteger
  def generateId: Int = atomic.incrementAndGet
  def makeSuccess[A]: Either[A, Unit] = Right(())
}
