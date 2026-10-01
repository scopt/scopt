package scopt

import collection.mutable.ListBuffer
import collection.immutable.ListMap
import scala.collection.{ Seq => CSeq }
import scala.collection.immutable.{ Seq => ISeq }
import OptionDef._
import OptionDefKind._
import OEffect._

private[scopt] object ORunner {
  private[scopt] def renderUsage[C](
      mode: RenderingMode,
      options: List[OptionDef[_, C]]
  ): (String, String) = {

    def heads: ISeq[OptionDef[_, C]] = options filter { _.kind == Head }
    def arguments: ISeq[OptionDef[_, C]] = options filter { _.kind == Arg }
    def commands: ISeq[OptionDef[_, C]] = options filter { _.kind == Cmd }
    def programNames: ISeq[OptionDef[_, C]] = options filter { _.kind == ProgramName }
    def programName: String = programNames.headOption match {
      case Some(option: OptionDef[_, C]) => option.desc
      case _                             => ""
    }

    def optionsForRender: List[OptionDef[_, C]] = {
      val unsorted = options filter { o =>
        o.kind != Head &&
        o.kind != Check &&
        o.kind != ProgramName &&
        !o.isHidden
      }
      val (remaining, sorted) = unsorted partition { _.hasParent } match {
        case (p, np) => (ListBuffer() ++ p, ListBuffer() ++ np)
      }
      var continue = true
      while (!remaining.isEmpty && continue) {
        continue = false
        for {
          parent <- sorted.toList
        } {
          val childrenOfThisParent = remaining filter { _.getParentId == Some(parent.id) }
          if (childrenOfThisParent.nonEmpty) {
            remaining --= childrenOfThisParent
            sorted.insertAll((sorted indexOf parent) + 1, childrenOfThisParent)
            continue = true
          }
        }
      }
      sorted.toList
    }

    def itemUsage(value: OptionDef[_, C]): String = {

      def indentDescription(desc: String): String = desc.split(NL).mkString(NLTB)

      value.kind match {
        case ProgramName         => value.desc
        case Head | Note | Check => value.desc
        case Cmd                 =>
          "Command: " + commandExample(Some(value)) + NL + value.desc
        case Arg                          => WW + value.name + NLTB + value.desc
        case Opt if value.read.arity == 2 =>
          WW + (value.shortOpt map { o =>
            "-" + o + ":" + value.keyValueString + " | "
          } getOrElse { "" }) +
            value.fullName + ":" + value.keyValueString + NLTB + indentDescription(value.desc)
        case Opt if value.read.arity == 1 =>
          WW + (value.shortOpt map { o =>
            "-" + o + " " + value.valueString + " | "
          } getOrElse { "" }) +
            value.fullName + " " + value.valueString + NLTB + indentDescription(value.desc)
        case Opt | OptHelp | OptVersion =>
          WW + (value.shortOpt map { o =>
            "-" + o + " | "
          } getOrElse { "" }) +
            value.fullName + NLTB + indentDescription(value.desc)
      }
    }

    lazy val header = (heads map { itemUsage }).mkString(NL)

    def usageColumn1(value: OptionDef[_, C]): String =
      value.kind match {
        case ProgramName         => value.desc
        case Head | Note | Check => ""
        case Cmd                 =>
          "Command: " + commandExample(Some(value)) + NL
        case Arg                          => WW + value.name
        case Opt if value.read.arity == 2 =>
          WW + (value.shortOpt map { o =>
            "-" + o + ", "
          } getOrElse { "" }) +
            value.fullName + ":" + value.keyValueString
        case Opt if value.read.arity == 1 =>
          WW + (value.shortOpt map { o =>
            "-" + o + ", "
          } getOrElse { "" }) +
            value.fullName + " " + value.valueString
        case Opt | OptHelp | OptVersion =>
          WW + (value.shortOpt map { o =>
            "-" + o + ", "
          } getOrElse { "" }) +
            value.fullName
      }

    def usageTwoColumn(value: OptionDef[_, C], col1Length: Int): String = {
      def spaceToDesc(str: String, description: String) = {
        if (description.isEmpty) str
        else {
          if ((str.length + WW.length) <= col1Length)
            str + (" " * (col1Length - str.length)) + description
              .split(NL)
              .mkString(NL + " " * col1Length)
          else
            str + NL + description.split(NL).map(s => (" " * col1Length) + s).mkString(NL)
        }
      }
      value.kind match {
        case ProgramName                  => value.desc
        case Head | Note | Check          => value.desc
        case Cmd                          => usageColumn1(value) + value.desc
        case Arg                          => spaceToDesc(usageColumn1(value), value.desc)
        case Opt if value.read.arity == 2 => spaceToDesc(usageColumn1(value), value.desc)
        case Opt if value.read.arity == 1 => spaceToDesc(usageColumn1(value), value.desc)
        case Opt | OptHelp | OptVersion   => spaceToDesc(usageColumn1(value), value.desc)
      }
    }

    def renderOneColumnUsage: String = {
      val descriptions = optionsForRender map { itemUsage }
      (if (header == "") "" else header + NL) +
        (usageExample match {
          case "" => ""
          case x  => "Usage: " + x + NLNL
        }) +
        descriptions.mkString(NL)
    }

    def renderTwoColumnsUsage: String = {
      val xs = optionsForRender
      val descriptions = {
        val col1Len = math.min(
          column1MaxLength,
          xs map { x =>
            usageColumn1(x).length + WW.length
          } match {
            case Nil  => 0
            case list => list.max
          }
        )
        xs map { x =>
          usageTwoColumn(x, col1Len)
        }
      }
      (if (header == "") "" else header + NL) +
        (usageExample match {
          case "" => ""
          case x  => "Usage: " + x + NLNL
        }) +
        descriptions.mkString(NL)
    }

    def commandExample(cmd: Option[OptionDef[_, C]]): String = {
      def commandName(cmd: OptionDef[_, C]): String =
        (cmd.getParentId map { x =>
          (commands find { _.id == x } map { commandName } getOrElse { "" }) + " "
        } getOrElse { "" }) + cmd.name

      val text = new ListBuffer[String]()
      text += cmd map { commandName } getOrElse programName
      val parentId = cmd map { _.id }
      val cs = commands filter { c =>
        c.getParentId == parentId && !c.isHidden
      }
      if (cs.nonEmpty) text += cs map { _.name } mkString ("[", "|", "]")
      val os = options.toSeq filter { case x =>
        (x.kind == Opt || x.kind == OptVersion || x.kind == OptHelp) &&
        x.getParentId == parentId
      }
      val as = arguments filter { _.getParentId == parentId }
      if (os.nonEmpty) text += "[options]"
      if (cs exists { case x => arguments exists { _.getParentId == Some(x.id) } })
        text += "<args>..."
      else if (as.nonEmpty) text ++= as map { _.argName }
      text.mkString(" ")
    }

    def usageExample: String = commandExample(None)

    val usg = mode match {
      case RenderingMode.OneColumn  => renderOneColumnUsage
      case RenderingMode.TwoColumns => renderTwoColumnsUsage
    }
    (header, usg)
  }

  /** parses the given `args`.
   */
  private[scopt] def runParser[C](
      args: CSeq[String],
      init: C,
      options: List[OptionDef[_, C]],
      setup: OParserSetup
  ): (Option[C], List[OEffect]) = {
    var i = 0
    import setup._
    def heads: ISeq[OptionDef[_, C]] = options filter { _.kind == Head }
    def nonArgs: ISeq[OptionDef[_, C]] = options filter { case x =>
      x.kind == Opt ||
      x.kind == Note ||
      x.kind == OptHelp ||
      x.kind == OptVersion
    }
    def arguments: ISeq[OptionDef[_, C]] = options filter { _.kind == Arg }
    def commands: ISeq[OptionDef[_, C]] = options filter { _.kind == Cmd }
    def checks: ISeq[OptionDef[_, C]] = options filter { _.kind == Check }
    lazy val helpOptions: ISeq[OptionDef[_, C]] = options filter { _.kind == OptHelp }

    val pendingOptions = ListBuffer() ++ (nonArgs filterNot { _.hasParent })
    val pendingArgs = ListBuffer() ++ (arguments filterNot { _.hasParent })
    val pendingCommands = ListBuffer() ++ (commands filterNot { _.hasParent })
    var occurrences: Map[OptionDef[_, C], Int] = ListMap[OptionDef[_, C], Int]().withDefaultValue(0)
    var _config: C = init
    var _error = false
    lazy val (header0, usage0) = ORunner.renderUsage(renderingMode, options)

    val effects = ListBuffer[OEffect]()
    def displayToOut(msg: String): Unit = effects.append(DisplayToOut(msg))
    def displayToErr(msg: String): Unit = effects.append(DisplayToErr(msg))
    def reportError(msg: String): Unit = effects.append(ReportError(msg))
    def reportWarning(msg: String): Unit = effects.append(ReportWarning(msg))
    def terminate(exitState: Either[String, Unit]): Unit = effects.append(Terminate(exitState))

    def pushChildren(opt: OptionDef[_, C]): Unit = {
      // commands are cleared to guarantee that it appears first
      pendingCommands.clear()

      pendingOptions insertAll (
        0,
        nonArgs filter { x =>
          x.getParentId == Some(opt.id) &&
          !pendingOptions.contains(x)
        }
      )
      pendingArgs insertAll (
        0,
        arguments filter { x =>
          x.getParentId == Some(opt.id) &&
          !pendingArgs.contains(x)
        }
      )
      pendingCommands insertAll (
        0,
        commands filter { x =>
          x.getParentId == Some(opt.id) &&
          !pendingCommands.contains(x)
        }
      )
    }
    def handleError(msg: String): Unit = {
      if (errorOnUnknownArgument) {
        _error = true
        reportError(msg)
      } else reportWarning(msg)
    }
    lazy val helpAction: C => C = { (c: C) =>
      displayToOut(usage0)
      terminate(Right(()))
      c
    }
    lazy val versionAction: C => C = { (c: C) =>
      displayToOut(header0)
      terminate(Right(()))
      c
    }
    def handleArgument(opt: OptionDef[_, C], arg: String): Unit = {
      // version and help are dependent on runner results, so the actions are provided here
      val runnerAction = opt.kind match {
        case OptHelp    => Some(helpAction)
        case OptVersion => Some(versionAction)
        case _          => None
      }
      opt.applyArgument(arg, _config, runnerAction) match {
        case Right(c) =>
          _config = c
          pushChildren(opt)
        case Left(xs) =>
          _error = true
          xs foreach reportError
      }
    }
    def handleVariadicArgument(opt: OptionDef[_, C], tokens: CSeq[String]): Unit = {
      val runnerAction = opt.kind match {
        case OptHelp    => Some(helpAction)
        case OptVersion => Some(versionAction)
        case _          => None
      }
      opt.applyVariadic(tokens, _config, runnerAction) match {
        case Right(c) =>
          _config = c
          pushChildren(opt)
        case Left(xs) =>
          _error = true
          xs foreach reportError
      }
    }
    def handleFallback[A](opt: OptionDef[A, C], arg: A): Unit = {
      // version and help are dependent on runner results, so the actions are provided here
      val runnerAction = opt.kind match {
        case OptHelp    => Some(helpAction)
        case OptVersion => Some(versionAction)
        case _          => None
      }
      opt.applyFallback(arg, _config, runnerAction) match {
        case Right(c) =>
          _config = c
          pushChildren(opt)
        case Left(xs) =>
          _error = true
          xs foreach reportError
      }
    }
    def handleOccurrence(opt: OptionDef[_, C], pending: ListBuffer[OptionDef[_, C]]): Unit = {

      if (occurrences.contains(opt)) {
        val o = occurrences(opt) + 1
        occurrences += (opt -> o)
      } else {
        occurrences += (opt -> 1)
      }

      if (occurrences(opt) >= opt.getMaxOccurs) {
        pending -= opt
      }
    }
    def findCommand(cmd: String): Option[OptionDef[_, C]] =
      pendingCommands find { _.name == cmd }
    def variadicTokensToRead(i: Int): Int = {
      def isStop(j: Int): Boolean =
        j >= args.length ||
          pendingOptions.exists(_.tokensToRead(j, args) > 0) ||
          pendingCommands.exists(_.name == args(j)) ||
          args(j) == "--"
      Iterator.from(i + 1).takeWhile(j => !isStop(j)).length
    }
    // greedy match
    def handleShortOptions(g0: String): Unit = {
      val gs = (0 to g0.size - 1).toSeq map { n =>
        g0.substring(0, g0.size - n)
      }
      gs flatMap { g =>
        pendingOptions map { (g, _) }
      } find { case (g, opt) =>
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
      Validation.validateValue(checks flatMap { _.checks })(c) match {
        case Right(c) => // do nothing
        case Left(xs) =>
          _error = true
          xs foreach reportError
      }
    }
    def showUsageAsError(): Unit = {
      displayToErr(usage0)
    }
    def showTryHelp(): Unit = {
      def oxford(xs: List[String]): String = xs match {
        case a :: b :: Nil => a + " or " + b
        case _             => (xs.dropRight(2) :+ xs.takeRight(2).mkString(", or ")).mkString(", ")
      }
      if (helpOptions.nonEmpty) {
        displayToErr("Try " + oxford(helpOptions.toList map {
          _.fullName
        }) + " for more information.")
      }
    }
    var processOptions = true

    while (i < args.length) {
      if (args(i) == "--") {
        processOptions = false
      } else {
        pendingOptions find {
          _.tokensToRead(i, args) > 0
        } match {
          case Some(option) if processOptions =>
            handleOccurrence(option, pendingOptions)

            if (option.read.isVariadic && option.tokensToRead(i, args) == 2) {
              val n = variadicTokensToRead(i)
              if (n == 0) {
                handleError("Missing value after " + args(i))
              } else {
                val tokens = args.slice(i + 1, i + 1 + n)
                handleVariadicArgument(option, tokens)
                i += n
              }
            } else {
              option(i, args) match {
                case Right(v)          => handleArgument(option, v)
                case Left(outOfBounds) => handleError(outOfBounds)
              }

              // move index forward for gobbling
              if (option.tokensToRead(i, args) > 1) {
                i += option.tokensToRead(i, args) - 1
              }
            }
          case _ =>
            def isShortOpt(arg: String): Boolean =
              arg.startsWith("-") && arg.length > 1 && arg(1) != '-'

            args(i) match {
              case arg if processOptions && arg.startsWith("--") =>
                handleError("Unknown option " + arg)
              case arg if processOptions && isShortOpt(arg) => handleShortOptions(arg drop 1)
              case arg if findCommand(arg).isDefined        =>
                val cmd = findCommand(arg).get
                handleOccurrence(cmd, pendingCommands)
                handleArgument(cmd, "")
              case arg if pendingArgs.isEmpty => handleError("Unknown argument '" + arg + "'")
              case arg                        =>
                val first = pendingArgs.head
                handleOccurrence(first, pendingArgs)
                handleArgument(first, arg)
            }
        }
      }
      i += 1
    }

    pendingOptions.filter(_.hasFallback).foreach { opt =>
      val fallback = opt.getFallback
      if (fallback != null) {
        handleOccurrence(opt, pendingOptions)
        handleFallback[Any](opt.asInstanceOf[OptionDef[Any, C]], fallback)
      }
    }
    (pendingOptions filter { opt =>
      opt.getMinOccurs > occurrences(opt)
    }) foreach { opt =>
      if (opt.getMinOccurs == 1) reportError("Missing " + opt.shortDescription)
      else
        reportError(
          opt.shortDescription.capitalize + " must be given " + opt.getMinOccurs + " times"
        )
      _error = true
    }
    (pendingArgs filter { arg =>
      arg.getMinOccurs > occurrences(arg)
    }) foreach { arg =>
      if (arg.getMinOccurs == 1) reportError("Missing " + arg.shortDescription)
      else
        reportError(
          arg.shortDescription.capitalize + "' must be given " + arg.getMinOccurs + " times"
        )
      _error = true
    }
    handleChecks(_config)
    if (_error) {
      if (showUsageOnError.getOrElse(helpOptions.isEmpty)) showUsageAsError()
      else showTryHelp()
      (None, effects.toList)
    } else (Some(_config), effects.toList)
  }

  private[scopt] def runEffects[C](effects: List[OEffect], setup: OEffectSetup): Unit = {
    effects foreach {
      case DisplayToOut(msg)    => setup.displayToOut(msg)
      case DisplayToErr(msg)    => setup.displayToErr(msg)
      case ReportError(msg)     => setup.reportError(msg)
      case ReportWarning(msg)   => setup.reportWarning(msg)
      case Terminate(exitState) => setup.terminate(exitState)
    }
  }
}
