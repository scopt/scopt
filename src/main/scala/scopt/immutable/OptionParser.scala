package scopt.immutable

import scopt.generic._
import GenericOptionParser._

abstract case class OptionParser[C](
  programName: Option[String] = None,
  version: Option[String] = None,
  errorOnUnknownArgument: Boolean = true) extends GenericOptionParser[C] {

  def this() = this(None, None, true)
  def this(programName: String) = this(Some(programName), None, true)
  def this(programName: String, version: String) = this(Some(programName), Some(version), true)
  def this(errorOnUnknownArgument: Boolean) = this(None, None, errorOnUnknownArgument)
  def this(programName: String, errorOnUnknownArgument: Boolean) =
    this(Some(programName), None , errorOnUnknownArgument)

  /** adds a `String` option invoked by `-shortopt x` or `--longopt x`.
   * @param shortopt short option
   * @param longopt long option
   * @param description description in the usage text
   * @param action callback function
   */
  def opt[C](shortopt: String, longopt: String, description: String)(action: (String, C) => C) =
    new ArgOptionDefinition(Some(shortopt), longopt, defaultValueName, description, action)

  /** adds a `String` option invoked by `--longopt x`.
   * @param longopt long option
   * @param description description in the usage text
   * @param action callback function
   */
  def opt(longopt: String, description: String)(action: (String, C) => C) =
    new ArgOptionDefinition(None, longopt, defaultValueName, description, action)

  /** adds a `String` option invoked by `-shortopt x` or `--longopt x`.
   * @param shortopt short option  
   * @param longopt long option
   * @param valueName value name in the usage text
   * @param description description in the usage text
   * @param action callback function
   */      
  def opt(shortopt: String, longopt: String, valueName: String,
      description: String)(action: (String, C) => C) =
    new ArgOptionDefinition(Some(shortopt), longopt, valueName, description, action)

  /** adds a `String` option invoked by `-shortopt x` or `--longopt x`.
   * @param shortopt short option, or `None`  
   * @param longopt long option
   * @param valueName value name in the usage text
   * @param description description in the usage text
   * @param action callback function
   */  
  def opt(shortopt: Option[String], longopt: String, valueName: String,
      description: String)(action: (String, C) => C) =
    new ArgOptionDefinition(shortopt, longopt, valueName, description, action)

  /** adds a flag option invoked by `-shortopt` or `--longopt`.
   * @param shortopt short option
   * @param longopt long option
   * @param description description in the usage text
   * @param action callback function
   */      
  def flag(shortopt: String, longopt: String, description: String)(action: C => C) =
    new FlagOptionDefinition(Some(shortopt), longopt, description, action)

  /** adds a flag option invoked by `--longopt`.
   * @param longopt long option
   * @param description description in the usage text
   * @param action callback function
   */
  def flag(longopt: String, description: String)(action: C => C) =
    new FlagOptionDefinition(None, longopt, description, action)
      
  // we have to give these typed options separate names, because of &^@$! type erasure
  def intOpt(shortopt: String, longopt: String, description: String)(action: (Int, C) => C) =
    new IntArgOptionDefinition(Some(shortopt), longopt, defaultValueName, description, action)

  def intOpt(longopt: String, description: String)(action: (Int, C) => C) =
    new IntArgOptionDefinition(None, longopt, defaultValueName, description, action)
      
  def intOpt(shortopt: String, longopt: String, valueName: String,
      description: String)(action: (Int, C) => C) =
    new IntArgOptionDefinition(Some(shortopt), longopt, valueName, description, action)

  def intOpt(shortopt: Option[String], longopt: String, valueName: String,
      description: String)(action: (Int, C) => C) =
    new IntArgOptionDefinition(shortopt, longopt, valueName, description, action)
      
  def doubleOpt(shortopt: String, longopt: String, description: String)(action: (Double, C) => C) =
    new DoubleArgOptionDefinition(Some(shortopt), longopt, defaultValueName, description, action)

  def doubleOpt(longopt: String, description: String)(action: (Double, C) => C) =
    new DoubleArgOptionDefinition(None, longopt, defaultValueName, description, action)
      
  def doubleOpt(shortopt: String, longopt: String, valueName: String,
      description: String)(action: (Double, C) => C) =
    new DoubleArgOptionDefinition(Some(shortopt), longopt, valueName, description, action)

  def doubleOpt(shortopt: Option[String], longopt: String, valueName: String,
      description: String)(action: (Double, C) => C) =
    new DoubleArgOptionDefinition(shortopt, longopt, valueName, description, action)
    
  def booleanOpt(shortopt: String, longopt: String, description: String)(action: (Boolean, C) => C) =
    new BooleanArgOptionDefinition(Some(shortopt), longopt, defaultValueName, description, action)

  def booleanOpt(longopt: String, description: String)(action: (Boolean, C) => C) =
    new BooleanArgOptionDefinition(None, longopt, defaultValueName, description, action)
  
  def booleanOpt(shortopt: String, longopt: String, valueName: String,
      description: String)(action: (Boolean, C) => C) =
    new BooleanArgOptionDefinition(Some(shortopt), longopt, valueName, description, action)

  def booleanOpt(shortopt: Option[String], longopt: String, valueName: String,
      description: String)(action: (Boolean, C) => C) =
    new BooleanArgOptionDefinition(shortopt, longopt, valueName, description, action)
      
  def keyValueOpt(shortopt: String, longopt: String, description: String)(action: (String, String, C) => C) =
    new KeyValueArgOptionDefinition(Some(shortopt), longopt, defaultKeyName, defaultValueName, description, action)

  def keyValueOpt(longopt: String, description: String)(action: (String, String, C) => C) =
    new KeyValueArgOptionDefinition(None, longopt, defaultKeyName, defaultValueName, description, action)
  
  def keyValueOpt(shortopt: String, longopt: String, keyName: String, valueName: String,
      description: String)(action: (String, String, C) => C) =
    new KeyValueArgOptionDefinition(Some(shortopt), longopt, keyName, valueName, description, action)

  def keyValueOpt(shortopt: Option[String], longopt: String, keyName: String, valueName: String,
      description: String)(action: (String, String, C) => C) =
    new KeyValueArgOptionDefinition(shortopt, longopt, keyName, valueName, description, action)
  
  def keyIntValueOpt(shortopt: String, longopt: String, description: String)(action: (String, Int, C) => C) =
    new KeyIntValueArgOptionDefinition(Some(shortopt), longopt, defaultKeyName, defaultValueName, description, action)

  def keyIntValueOpt(longopt: String, description: String)(action: (String, Int, C) => C) =
    new KeyIntValueArgOptionDefinition(None, longopt, defaultKeyName, defaultValueName, description, action)
  
  def keyIntValueOpt(shortopt: String, longopt: String, keyName: String, valueName: String,
      description: String)(action: (String, Int, C) => C) =
    new KeyIntValueArgOptionDefinition(Some(shortopt), longopt, keyName, valueName, description, action)

  def keyIntValueOpt(shortopt: Option[String], longopt: String, keyName: String, valueName: String,
      description: String)(action: (String, Int, C) => C) =
    new KeyIntValueArgOptionDefinition(shortopt, longopt, keyName, valueName, description, action)
  
  def keyDoubleValueOpt(shortopt: String, longopt: String, description: String)(action: (String, Double, C) => C) =
    new KeyDoubleValueArgOptionDefinition(Some(shortopt), longopt, defaultKeyName, defaultValueName, description, action)

  def keyDoubleValueOpt(longopt: String, description: String)(action: (String, Double, C) => C) =
    new KeyDoubleValueArgOptionDefinition(None, longopt, defaultKeyName, defaultValueName, description, action)
    
  def keyDoubleValueOpt(shortopt: String, longopt: String, keyName: String, valueName: String,
      description: String)(action: (String, Double, C) => C) =
    new KeyDoubleValueArgOptionDefinition(Some(shortopt), longopt, keyName, valueName, description, action)

  def keyDoubleValueOpt(shortopt: Option[String], longopt: String, keyName: String, valueName: String,
      description: String)(action: (String, Double, C) => C) =
    new KeyDoubleValueArgOptionDefinition(shortopt, longopt, keyName, valueName, description, action)

  def keyBooleanValueOpt(shortopt: String, longopt: String, description: String)(action: (String, Boolean, C) => C) =
    new KeyBooleanValueArgOptionDefinition(Some(shortopt), longopt, defaultKeyName, defaultValueName, description, action)

  def keyBooleanValueOpt(longopt: String, description: String)(action: (String, Boolean, C) => C) =
    new KeyBooleanValueArgOptionDefinition(None, longopt, defaultKeyName, defaultValueName, description, action)

  def keyBooleanValueOpt(shortopt: String, longopt: String, keyName: String, valueName: String,
      description: String)(action: (String, Boolean, C) => C) =
    new KeyBooleanValueArgOptionDefinition(Some(shortopt), longopt, keyName, valueName, description, action)

  def keyBooleanValueOpt(shortopt: Option[String], longopt: String, keyName: String, valueName: String,
      description: String)(action: (String, Boolean, C) => C) =
    new KeyBooleanValueArgOptionDefinition(shortopt, longopt, keyName, valueName, description, action)
  
  def help(shortopt: String, longopt: String, description: String) =
    new FlagOptionDefinition[C](Some(shortopt), longopt, description, {_ => this.showUsage; exit})

  def help(shortopt: Option[String], longopt: String, description: String) =
    new FlagOptionDefinition[C](shortopt, longopt, description, {_ => this.showUsage; exit})
  
  def separator(description: String) =
    new SeparatorDefinition(description)
  
  /** adds an argument invoked by an option without `-` or `--`.
   * @param name name in the usage text
   * @param description description in the usage text
   * @param action callback function
   */  
  def arg(name: String, description: String)(action: (String, C) => C) =
    new Argument[C](name, description, 1, 1, action)

  /** adds an optional argument invoked by an option without `-` or `--`.
   * @param name name in the usage text
   * @param description description in the usage text
   * @param action callback function
   */  
  def argOpt(name: String, description: String)(action: (String, C) => C) =
    new Argument(name, description, 0, 1, action)
      
  /** adds a list of arguments invoked by options without `-` or `--`.
   * @param name name in the usage text
   * @param description description in the usage text
   * @param action callback function
   */
  def arglist(name: String, description: String)(action: (String, C) => C) =
    new Argument(name, description, 1, UNBOUNDED, action)

  /** adds an optional list of arguments invoked by options without `-` or `--`.
   * @param name name in the usage text
   * @param description description in the usage text
   * @param action callback function
   */
  def arglistOpt(name: String, description: String)(action: (String, C) => C) =
    new Argument(name, description, 0, UNBOUNDED, action)

}
