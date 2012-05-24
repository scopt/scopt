package scopt.generic

import GenericOptionParser.UNBOUNDED

// Base class for options.
// These are things that get listed when we ask for help,
// and optionally can accept string arguments & perform some kind of action,
// usually mutating a var.
private[scopt] case class OptionDefinition[C](
        canBeInvoked: Boolean,
        shortopt: Option[String],
        longopt: String,
        keyName: String,
        valueName: String,
        description: String,
        action: (String, C) => C,
        gobbleNextArgument: Boolean,
        keyValueArgument: Boolean,
        minOccurs: Int,
        maxOccurs: Int) {
  def shortDescription = "option " + longopt
}

// ----- Some standard option types ---------
private[scopt] class SeparatorDefinition[C](
        description: String
        ) extends OptionDefinition[C](false, null, null, null, null,
          description, { (a: String, c: C) => c }, false, false, 1, 1)

private[scopt] class Argument[C](
        name: String,
        description: String,
        minOccurs: Int,
        maxOccurs: Int,
        action: (String, C) => C
        ) extends OptionDefinition[C](false, null, name, null, name, 
          description, action, false, false, minOccurs, maxOccurs) {

  override def shortDescription = "argument " + name
}

private[scopt] class ArgOptionDefinition[C](
        shortopt: Option[String],
        longopt: String,
        valueName: String,
        description: String,
        action: (String, C) => C
        ) extends OptionDefinition[C](true, shortopt, longopt, null, valueName,
          description, action, true, false, 0, UNBOUNDED)

private[scopt] class IntArgOptionDefinition[C](
        shortopt: Option[String],
        longopt: String,
        valueName: String,
        description: String,
        action: (Int, C) => C
        ) extends OptionDefinition[C](true, shortopt, longopt, null, valueName,
          description, { (a: String, c: C) => action(a.toInt, c) }, true, false, 0, UNBOUNDED)

private[scopt] class DoubleArgOptionDefinition[C](
        shortopt: Option[String],
        longopt: String,
        valueName: String,
        description: String,
        action: (Double, C) => C
        ) extends OptionDefinition[C](true, shortopt, longopt, null, valueName,
          description, { (a: String, c: C) => action(a.toDouble, c) }, true, false, 0, UNBOUNDED)

private[scopt] class BooleanArgOptionDefinition[C](
        shortopt: Option[String],
        longopt: String,
        valueName: String,
        description: String,
        action: (Boolean, C) => C
        ) extends OptionDefinition[C](true, shortopt, longopt, null, valueName, 
          description, { (a: String, c: C) =>
    val boolValue = a.toLowerCase match {
      case "true" => true
      case "false" => false
      case "yes" => true
      case "no" => false
      case "1" => true
      case "0" => false
      case _ =>
        throw new IllegalArgumentException("Expected a string I can interpret as a boolean")
    }
    action(boolValue, c) },
    true, false, 0, UNBOUNDED)
    
private[scopt] object KeyValueParser {
  def split(s: String): (String, String) = s.indexOf('=') match {
    case -1     => throw new IllegalArgumentException("Expected a key=value pair")
    case n: Int => (s.slice(0, n), s.slice(n + 1, s.length))
  }
}

private[scopt] class KeyValueArgOptionDefinition[C](
        shortopt: Option[String],
        longopt: String,
        keyName: String,
        valueName: String,
        description: String,
        action: (String, String, C) => C
        ) extends OptionDefinition[C](true, shortopt, longopt, keyName, valueName, 
          description, { (a: String, c: C) => action(KeyValueParser.split(a)._1, KeyValueParser.split(a)._2, c) },
          false, true, 0, UNBOUNDED)

private[scopt] class KeyIntValueArgOptionDefinition[C](
        shortopt: Option[String],
        longopt: String,
        keyName: String,
        valueName: String,
        description: String,
        action: (String, Int, C) => C
        ) extends OptionDefinition[C](true, shortopt, longopt, keyName, valueName, 
          description, { (a: String, c: C) => action(KeyValueParser.split(a)._1, KeyValueParser.split(a)._2.toInt, c) },
          false, true, 0, UNBOUNDED)

private[scopt] class KeyDoubleValueArgOptionDefinition[C](
        shortopt: Option[String],
        longopt: String,
        keyName: String,
        valueName: String,
        description: String,
        action: (String, Double, C) => C
        ) extends OptionDefinition[C](true, shortopt, longopt, keyName, valueName, 
          description, { (a: String, c: C) => action(KeyValueParser.split(a)._1, KeyValueParser.split(a)._2.toDouble, c) },
          false, true, 0, UNBOUNDED)

private[scopt] class KeyBooleanValueArgOptionDefinition[C](
        shortopt: Option[String],
        longopt: String,
        keyName: String,
        valueName: String,
        description: String,
        action: (String, Boolean, C) => C
        ) extends OptionDefinition[C](true, shortopt, longopt, null, valueName, 
          description, { (a: String, c: C) => 
            val x = KeyValueParser.split(a)
            val key = x._1
            val boolValue = x._2.toLowerCase match {
              case "true" => true
              case "false" => false
              case "yes" => true
              case "no" => false
              case "1" => true
              case "0" => false
              case _ =>
                throw new IllegalArgumentException("Expected a string I can interpret as a boolean")
            }
            action(key, boolValue, c)
          },
          false, true, 0, UNBOUNDED)
      
private[scopt] class FlagOptionDefinition[C](
        shortopt: Option[String],
        longopt: String,
        description: String,
        action: C => C
        ) extends OptionDefinition[C](true, shortopt, longopt, null, null,
          description, { (a: String, c: C) => action(c) }, false, false, 0, UNBOUNDED)

private[scopt] trait GenericOptionParser[C] {
  def options: Seq[OptionDefinition[C]]
  def version: Option[String]
  def programName: Option[String]
  def errorOnUnknownArgument: Boolean

  protected def opts: Seq[OptionDefinition[C]] = options filter {!_.isInstanceOf[Argument[C]]}
  protected def arguments: Seq[Argument[C]] = options collect { case a: Argument[C] if a.maxOccurs <= 1 => a }
  protected def argList: Option[Argument[C]] = options collect { case a: Argument[C] if a.maxOccurs > 1 => a } headOption

  import GenericOptionParser._

  // -------- Getting usage information ---------------
  private def descriptions: Seq[String] = opts.map(opt => opt match {
    //case x: Argument => x.longopt + " :" + NLTB + opt.description
    case x if !x.canBeInvoked => x.description
    case x if x.keyValueArgument =>
      (x.shortopt map { o => "-" + o + ":" + x.keyName + "=" + x.valueName + " | " } getOrElse { "" }) +
      "--" + x.longopt + ":" + x.keyName + "=" + x.valueName + NLTB + x.description    
    case x if x.gobbleNextArgument =>
      (x.shortopt map { o => "-" + o + " " + x.valueName + " | " } getOrElse { "" }) +
      "--" + x.longopt + " " + x.valueName + NLTB + x.description
    case _ =>
      (opt.shortopt map { o => "-" + o + " | " } getOrElse { "" }) + 
      "--" + opt.longopt + NLTB + opt.description
  }) ++ (argList match {
    case Some(x: Argument[C]) => List(x.valueName + NLTB + x.description)
    case None                 => arguments.map(a => a.valueName + NLTB + a.description)
  })
  
  def usage: String = {
    val prorgamText = programName map { _ + " " } getOrElse { "" }
    val versionText = programName map { pg =>
      version map { NL + pg + " " + _ } getOrElse { "" }
    } getOrElse { "" }
    val optionText = if (opts.isEmpty) {""} else {"[options] "}
    val argumentList = argumentNames.mkString(" ")

    versionText + NL + "Usage: " + prorgamText + optionText + argumentList + NLNL +
    "  " + descriptions.mkString(NL + "  ") + NL
  }

  def showUsage = Console.err.println(usage)

  private def argumentNames: Seq[String] = argList match {
    case Some(x: Argument[C]) => List(x.valueName)
    case None                 => arguments.map(_.valueName)
  }

  private def applyArgument(option: OptionDefinition[C], arg: String, config: C): Option[C] =
    try {
      Some(option.action.apply(arg, config))
    } catch {
      case e: NumberFormatException =>
        System.err.println("Error: " + option.shortDescription + " expects a number but was given '" + arg + "'")
        None
      case e: Throwable =>
        System.err.println("Error: " + option.shortDescription + " failed when given '" + arg + "'. " + e.getMessage)
        None
    }

  /** parses the given `args`.
   * @return `true` if successful, `false` otherwise
   */
  def parse(args: Seq[String], init: C): Option[C] = {
    import collection.mutable.ListBuffer

    var i = 0
    val unseenArgs = ListBuffer() ++ arguments
    var argListCount = 0
    var indexOutOfBounds = false
    var _config: C = init
    var _error = false

    while (i < args.length) {
      val arg = args(i)
      val matchingOption = opts.find(opt =>
        opt.canBeInvoked &&
          ((!opt.keyValueArgument &&
            (arg == "--" + opt.longopt ||
            (opt.shortopt map { o => arg == "-" + o } getOrElse { false }))) ||
          (opt.keyValueArgument &&
            (arg.startsWith("--" + opt.longopt + ":") ||
            (opt.shortopt map { o => arg.startsWith("-" + o + ":") } getOrElse { false }))))
      )
      
      matchingOption match {
        case None =>
          if (arg.startsWith("-")) {
            if (errorOnUnknownArgument) {
              System.err.println("Error: Unknown argument '" + arg + "'")
              _error = true              
            } else
              System.err.println("Warning: Unknown argument '" + arg + "'")
          } else if (argList.isDefined) {
            argListCount += 1
            applyArgument(argList.get, arg, _config) match {
              case Some(c) => _config = c
              case None    => _error = true
            }
          } else if (unseenArgs.isEmpty) {
            if (errorOnUnknownArgument) {
              System.err.println("Error: Unknown argument '" + arg + "'")
              _error = true             
            } else
              System.err.println("Warning: Unknown argument '" + arg + "'")
          } else {
            val first = unseenArgs.remove(0)
            applyArgument(first, arg, _config) match {
              case Some(c) => _config = c
              case None    => _error = true
            }
          }
          
        case Some(option) =>
          val argToPass: String = if (option.gobbleNextArgument) {
            i += 1;
            
            if (i >= args.length) {
              indexOutOfBounds = true
              if (errorOnUnknownArgument) {
                System.err.println("Error: missing value after '" + arg + "'")
                _error = true
              } else
                System.err.println("Warning: missing value after '" + arg + "'")
              ""
            } else
              args(i)
          } else if (option.keyValueArgument &&
              (option.shortopt map { o => arg.startsWith("-" + o + ":") } getOrElse { false })) {
            arg.drop(("-" + option.shortopt.get + ":").length)
          } else if (option.keyValueArgument &&
              arg.startsWith("--" + option.longopt + ":")) {
            arg.drop(("--" + option.longopt + ":").length)
          } else
            ""
          
          if (!indexOutOfBounds) {
            applyArgument(option, argToPass, _config) match {
              case Some(c) => _config = c
              case None    => _error = true
            }
          }
      }
      i += 1
    }
    
    if ((unseenArgs.toList exists { _.minOccurs > 0 }) ||
        (argListCount == 0 && (argList match {
          case Some(a: Argument[Unit]) => a.minOccurs > 0
          case _ => false
        }))) {
      System.err.println("Error: missing arguments: " + argumentNames.mkString(", "))
      _error = true
    }
    if (_error) {
      showUsage
      None
    }
    else Some(_config)
  }
}

private[scopt] object GenericOptionParser {
  val UNBOUNDED = 1024
  val NL = System.getProperty("line.separator")
  val TB = "        "
  val NLTB = NL + TB
  val NLNL = NL + NL
  val defaultKeyName = "<key>"
  val defaultValueName = "<value>"
}
