package scopt

import org.log4s._

trait OParserSetup {
  def renderingMode: RenderingMode
  def errorOnUnknownArgument: Boolean

  /**
   * Show usage text on parse error.
   * Defaults to None, which displays the usage text if
   * --help option is not defined.
   */
  def showUsageOnError: Option[Boolean]
  def displayToOut(msg: String): Unit
  def displayToErr(msg: String): Unit
  def displayToWarn(msg: String): Unit
  def reportError(msg: String): Unit
  def reportWarning(msg: String): Unit
  def terminate(exitState: Either[String, Unit]): Unit
}

abstract class DefaultOParserSetup extends OParserSetup {
  private[this] val logger = getLogger
  override def renderingMode: RenderingMode = RenderingMode.TwoColumns
  override def errorOnUnknownArgument: Boolean = true
  override def showUsageOnError: Option[Boolean] = None
  override def displayToOut(msg: String): Unit = {
    logger.info(msg)
  }
  override def displayToErr(msg: String): Unit = {
    logger.error(msg)
  }
  override def displayToWarn(msg: String): Unit = {
    logger.warn(msg)
  }
  override def reportError(msg: String): Unit = {
    displayToErr(msg)
  }
  override def reportWarning(msg: String): Unit = {
    displayToWarn(msg)
  }
  override def terminate(exitState: Either[String, Unit]): Unit =
    exitState match {
      case Left(_)  => sys.exit(1)
      case Right(_) => sys.exit(0)
    }
}
