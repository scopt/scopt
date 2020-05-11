package scopt

import scala.collection.{ Seq => CSeq }

private[scopt] sealed trait OptionDefKind {}
private[scopt] object OptionDefKind {
  case object Opt extends OptionDefKind
  case object Note extends OptionDefKind
  case object Arg extends OptionDefKind
  case object Cmd extends OptionDefKind
  case object Head extends OptionDefKind
  case object Check extends OptionDefKind
  case object ProgramName extends OptionDefKind
  case object OptVersion extends OptionDefKind
  case object OptHelp extends OptionDefKind
}

class OptionDef[A: Read, C](
    _id: Int,
    _kind: OptionDefKind,
    _name: String,
    _shortOpt: Option[String],
    _keyName: Option[String],
    _valueName: Option[String],
    _desc: String,
    _action: (A, C) => C,
    _validations: CSeq[A => Either[String, Unit]],
    _configValidations: CSeq[C => Either[String, Unit]],
    _parentId: Option[Int],
    _minOccurs: Int,
    _maxOccurs: Int,
    _isHidden: Boolean,
    _defaultValue: Option[A],
    _fallback: Option[() => A],
    _defCallback: OptionDefCallback[C]) {

  import platform._
  import OptionDef._
  import OptionDefKind._

  def this(kind: OptionDefKind, name: String, defCallback: OptionDefCallback[C]) =
    this(
      _id = OptionDef.generateId,
      _kind = kind,
      _name = name,
      _shortOpt = None,
      _keyName = None,
      _valueName = None,
      _desc = "",
      _action = { (a: A, c: C) =>
        c
      },
      _validations = CSeq(),
      _configValidations = CSeq(),
      _parentId = None,
      _minOccurs = 0,
      _maxOccurs = 1,
      _isHidden = false,
      _defaultValue = None,
      _fallback = None,
      _defCallback = defCallback
    )

  def this(kind: OptionDefKind, name: String) =
    this(kind, name, OptionDefCallback.nullCallback[C])

  private[scopt] def copy(
      _id: Int = this._id,
      _kind: OptionDefKind = this._kind,
      _name: String = this._name,
      _shortOpt: Option[String] = this._shortOpt,
      _keyName: Option[String] = this._keyName,
      _valueName: Option[String] = this._valueName,
      _desc: String = this._desc,
      _action: (A, C) => C = this._action,
      _validations: CSeq[A => Either[String, Unit]] = this._validations,
      _configValidations: CSeq[C => Either[String, Unit]] = this._configValidations,
      _parentId: Option[Int] = this._parentId,
      _minOccurs: Int = this._minOccurs,
      _maxOccurs: Int = this._maxOccurs,
      _isHidden: Boolean = this._isHidden,
      _defaultValue: Option[A] = this._defaultValue,
      _fallback: Option[() => A] = this._fallback,
      _defCallback: OptionDefCallback[C] = this._defCallback): OptionDef[A, C] =
    new OptionDef(
      _id = _id,
      _kind = _kind,
      _name = _name,
      _shortOpt = _shortOpt,
      _keyName = _keyName,
      _valueName = _valueName,
      _desc = _desc,
      _action = _action,
      _validations = _validations,
      _configValidations = _configValidations,
      _parentId = _parentId,
      _minOccurs = _minOccurs,
      _maxOccurs = _maxOccurs,
      _isHidden = _isHidden,
      _defaultValue = _defaultValue,
      _fallback = _fallback,
      _defCallback = _defCallback
    )

  private[scopt] def read: Read[A] = implicitly[Read[A]]

  private[this] def fireChange[A: Read](value: OptionDef[A, C]): OptionDef[A, C] = {
    _defCallback.onChange(value)
    value
  }

  /** Adds a callback function. */
  def action(f: (A, C) => C): OptionDef[A, C] =
    fireChange(copy(_action = (a: A, c: C) => { f(a, _action(a, c)) }))

  /** Adds a callback function. */
  def foreach(f: A => Unit): OptionDef[A, C] =
    fireChange(copy(_action = (a: A, c: C) => {
      val c2 = _action(a, c)
      f(a)
      c2
    }))

  override def toString: String = fullName

  /** Adds short option -x. */
  def abbr(x: String): OptionDef[A, C] =
    fireChange(copy(_shortOpt = Some(x)))

  /** Requires the option to appear at least `n` times. */
  def minOccurs(n: Int): OptionDef[A, C] =
    fireChange(copy(_minOccurs = n))

  /** Allows the argument to appear at most `n` times. */
  def maxOccurs(n: Int): OptionDef[A, C] =
    fireChange(copy(_maxOccurs = n))

  /** Requires the option to appear at least once. */
  def required(): OptionDef[A, C] = minOccurs(1)

  /** Changes the option to be optional. */
  def optional(): OptionDef[A, C] = minOccurs(0)

  /** Allows the argument to appear multiple times. */
  def unbounded(): OptionDef[A, C] = maxOccurs(UNBOUNDED)

  /** Hides the option in any usage text. */
  def hidden(): OptionDef[A, C] =
    fireChange(copy(_isHidden = true))

  /** Adds description in the usage text. */
  def text(x: String): OptionDef[A, C] =
    fireChange(copy(_desc = x))

  /** Adds value name used in the usage text. */
  def valueName(x: String): OptionDef[A, C] =
    fireChange(copy(_valueName = Some(x)))

  /** Adds key name used in the usage text. */
  def keyName(x: String): OptionDef[A, C] =
    fireChange(copy(_keyName = Some(x)))

  /** Adds key and value names used in the usage text. */
  def keyValueName(k: String, v: String): OptionDef[A, C] =
    keyName(k).valueName(v)

  /** Adds custom validation. */
  def validate(f: A => Either[String, Unit]) =
    fireChange(copy(_validations = _validations :+ f))

  /** */
  def withDefault(value: A): OptionDef[A, C] =
    fireChange(copy(_defaultValue = Option(value)))

  /** provides a default to fallback to, e.g. for System.getenv */
  def withFallback(to: () => A): OptionDef[A, C] =
    fireChange(copy(_fallback = Option(to)))

  private[scopt] def validateConfig(f: C => Either[String, Unit]) =
    fireChange(copy(_configValidations = _configValidations :+ f))
  private[scopt] def parent(x: OptionDef[_, C]): OptionDef[A, C] =
    fireChange(copy(_parentId = Some(x.id)))

  /** Adds opt/arg under this command. */
  def children(xs: OptionDef[_, C]*): OptionDef[A, C] = {
    xs foreach { _.parent(this) }
    this
  }

  private[scopt] val kind: OptionDefKind = _kind
  private[scopt] val id: Int = _id
  val name: String = _name
  private[scopt] def action: (A, C) => C = _action
  def getMinOccurs: Int = _minOccurs
  def getMaxOccurs: Int = _maxOccurs
  private[scopt] def shortOptOrBlank: String = _shortOpt getOrElse ("")
  private[scopt] def hasParent: Boolean = _parentId.isDefined
  private[scopt] def getParentId: Option[Int] = _parentId
  def isHidden: Boolean = _isHidden
  def hasDefault: Boolean = _defaultValue.isDefined
  def getDefault: A = _defaultValue.get
  def hasFallback: Boolean = _fallback.isDefined
  def getFallback: A = _fallback.get.apply
  private[scopt] def checks: CSeq[C => Either[String, Unit]] = _configValidations
  def desc: String = _desc
  def shortOpt: Option[String] = _shortOpt
  def valueName: Option[String] = _valueName

  // apply the option with the passed in value
  private[scopt] def applyArgument(
      arg: String,
      config: C,
      onOption: Option[C => C]): Either[CSeq[String], C] =
    try {
      val x = read.reads(arg)
      Validation.validateValue(_validations)(x) match {
        case Right(_) =>
          onOption match {
            case Some(f) => Right(f(config))
            case _       => Right(action(x, config))
          }
        case Left(xs) => Left(xs)
      }
    } catch applyArgumentExHandler(shortDescription.capitalize, arg)

  // apply the option with the passed in value
  private[scopt] def applyDefault(config: C, onOption: Option[C => C]): Either[CSeq[String], C] = {
    val x = getDefault
    Validation.validateValue(_validations)(x) match {
      case Right(_) =>
        onOption match {
          case Some(f) => Right(f(config))
          case _       => Right(action(x, config))
        }
      case Left(xs) => Left(xs)
    }
  }

  // number of tokens to read: 0 for no match, 2 for "--foo 1", 1 for "--foo:1"
  private[scopt] def shortOptTokens(arg: String): Int =
    _shortOpt match {
      case Some(c) if arg == "-" + shortOptOrBlank                 => 1 + read.tokensToRead
      case Some(c) if arg startsWith ("-" + shortOptOrBlank + ":") => 1
      case Some(c) if arg startsWith ("-" + shortOptOrBlank + "=") => 1
      case _                                                       => 0
    }
  private[scopt] def longOptTokens(arg: String): Int =
    if (arg == fullName) 1 + read.tokensToRead
    else if ((arg startsWith (fullName + ":")) || (arg startsWith (fullName + "="))) 1
    else 0
  private[scopt] def isOptLike: Boolean =
    (kind == Opt || kind == OptHelp || kind == OptVersion)
  private[scopt] def tokensToRead(i: Int, args: CSeq[String]): Int =
    if (i >= args.length || !isOptLike) 0
    else
      args(i) match {
        case arg if longOptTokens(arg) > 0  => longOptTokens(arg)
        case arg if shortOptTokens(arg) > 0 => shortOptTokens(arg)
        case _                              => 0
      }
  private[scopt] def apply(i: Int, args: CSeq[String]): Either[String, String] =
    if (i >= args.length || !isOptLike) Left("Option does not match")
    else
      args(i) match {
        case arg if longOptTokens(arg) == 2 || shortOptTokens(arg) == 2 =>
          token(i + 1, args) map { Right(_) } getOrElse Left("Missing value after " + arg)
        case arg if longOptTokens(arg) == 1 && read.tokensToRead == 1 =>
          Right(arg drop (fullName + ":").length)
        case arg if shortOptTokens(arg) == 1 && read.tokensToRead == 1 =>
          Right(arg drop ("-" + shortOptOrBlank + ":").length)
        case _ => Right("")
      }
  private[scopt] def token(i: Int, args: CSeq[String]): Option[String] =
    if (i >= args.length || !isOptLike) None
    else Some(args(i))

  private[scopt] def keyValueString: String =
    (_keyName getOrElse defaultKeyName) + "=" + valueString
  private[scopt] def valueString: String = (_valueName getOrElse defaultValueName)
  def shortDescription: String =
    kind match {
      case k if isOptLike => "option " + fullName
      case Cmd            => "command " + fullName
      case _              => "argument " + fullName
    }
  def fullName: String =
    kind match {
      case k if isOptLike => "--" + name
      case _              => name
    }
  private[scopt] def argName: String =
    kind match {
      case Arg if getMinOccurs == 0 => "[" + fullName + "]"
      case _                        => fullName
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

abstract class OptionDefCallback[C] {
  private[scopt] def onChange[A: Read](value: OptionDef[A, C]): Unit
}
object OptionDefCallback {
  def nullCallback[C] = new OptionDefCallback[C] {
    override private[scopt] def onChange[A: Read](value: OptionDef[A, C]): Unit = ()
  }
}
