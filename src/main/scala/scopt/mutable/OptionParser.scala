package scopt.mutable

import scopt.generic._

/** OptionParser is instantiated within your object,
 * set up by an (ordered) sequence of invocations of 
 * the various builder methods such as
 * <a href="#opt(String,String,String,String,(String) ⇒ Unit):Unit"><code>opt</code></a> method or
 * <a href="#arg(String,String,(String) ⇒ Unit):Unit"><code>arg</code></a> method.
 * {{{
 * val parser = new OptionParser("scopt") {
 *   intOpt("f", "foo", "foo is an integer property", {v: Int => config.foo = v})
 *   opt("o", "output", "<file>", "output is a string property", {v: String => config.bar = v})
 *   booleanOpt("xyz", "xyz is a boolean property", {v: Boolean => config.xyz = v})
 *   keyValueOpt("l", "lib", "<libname>", "<filename>", "load library <libname>",
 *     {(key: String, value: String) => { config.libname = key; config.libfile = value } })
 *   arg("<singlefile>", "<singlefile> is an argument", {v: String => config.whatnot = v})
 *   // arglist("<file>...", "arglist allows variable number of arguments",
 *   //   {v: String => config.files = (v :: config.files).reverse })
 * }
 * if (parser.parse(args)) {
 *   // do stuff
 * }
 * else {
 *   // arguments are bad, usage message will have been displayed
 * }
 * }}}
 */
case class OptionParser(
        programName: Option[String],
        version: Option[String],
        errorOnUnknownArgument: Boolean) extends GenericOptionParser[Unit] {
  import GenericOptionParser._

  def this() = this(None, None, true)
  def this(programName: String) = this(Some(programName), None, true)
  def this(programName: String, version: String) = this(Some(programName), Some(version), true)
  def this(errorOnUnknownArgument: Boolean) = this(None, None, errorOnUnknownArgument)
  def this(programName: String, errorOnUnknownArgument: Boolean) =
    this(Some(programName), None , errorOnUnknownArgument)
  
  val options = new scala.collection.mutable.ListBuffer[OptionDefinition[Unit]]

  /** parses the given `args`.
   * @return `true` if successful, `false` otherwise
   */
  def parse(args: Seq[String]): Boolean =
    parse(args, ()) match {
      case Some(x) => true
      case None    => false
    }

  // -------- Defining options ---------------
  protected def add(option: OptionDefinition[Unit]) {
    options += option
  }

  /** adds a `String` option invoked by `-shortopt x` or `--longopt x`.
   * @param shortopt short option
   * @param longopt long option
   * @param description description in the usage text
   * @param action callback function
   */
  def opt(shortopt: String, longopt: String, description: String, action: String => Unit) =
    add(new ArgOptionDefinition(Some(shortopt), longopt, defaultValueName, description,
      { (s: String, _) => action(s) }))

  /** adds a `String` option invoked by `--longopt x`.
   * @param longopt long option
   * @param description description in the usage text
   * @param action callback function
   */
  def opt(longopt: String, description: String, action: String => Unit) =
    add(new ArgOptionDefinition(None, longopt, defaultValueName, description,
      { (s: String, _) => action(s) }))

  /** adds a `String` option invoked by `-shortopt x` or `--longopt x`.
   * @param shortopt short option  
   * @param longopt long option
   * @param valueName value name in the usage text
   * @param description description in the usage text
   * @param action callback function
   */      
  def opt(shortopt: String, longopt: String, valueName: String,
      description: String, action: String => Unit) =
    add(new ArgOptionDefinition(Some(shortopt), longopt, valueName, description,
      { (s: String, _) => action(s) }))

  /** adds a `String` option invoked by `-shortopt x` or `--longopt x`.
   * @param shortopt short option, or `None`  
   * @param longopt long option
   * @param valueName value name in the usage text
   * @param description description in the usage text
   * @param action callback function
   */  
  def opt(shortopt: Option[String], longopt: String, valueName: String,
      description: String, action: String => Unit) =
    add(new ArgOptionDefinition(shortopt, longopt, valueName, description,
      { (s: String, _) => action(s) }))

  /** adds a flag option invoked by `-shortopt` or `--longopt`.
   * @param shortopt short option
   * @param longopt long option
   * @param description description in the usage text
   * @param action callback function
   */      
  def opt(shortopt: String, longopt: String, description: String, action: => Unit) =
    add(new FlagOptionDefinition(Some(shortopt), longopt, description, _ => action))

  /** adds a flag option invoked by `--longopt`.
   * @param longopt long option
   * @param description description in the usage text
   * @param action callback function
   */
  def opt(longopt: String, description: String, action: => Unit) =
    add(new FlagOptionDefinition(None, longopt, description, _ => action))
      
  // we have to give these typed options separate names, because of &^@$! type erasure
  def intOpt(shortopt: String, longopt: String, description: String, action: Int => Unit) =
    add(new IntArgOptionDefinition(Some(shortopt), longopt, defaultValueName, description,
      { (i: Int, _) => action(i) }))

  def intOpt(longopt: String, description: String, action: Int => Unit) =
    add(new IntArgOptionDefinition(None, longopt, defaultValueName, description,
      { (i: Int, _) => action(i) }))
      
  def intOpt(shortopt: String, longopt: String, valueName: String,
      description: String, action: Int => Unit) =
    add(new IntArgOptionDefinition(Some(shortopt), longopt, valueName, description,
      { (i: Int, _) => action(i) }))

  def intOpt(shortopt: Option[String], longopt: String, valueName: String,
      description: String, action: Int => Unit) =
    add(new IntArgOptionDefinition(shortopt, longopt, valueName, description,
      { (i: Int, _) => action(i) }))
      
  def doubleOpt(shortopt: String, longopt: String, description: String, action: Double => Unit) =
    add(new DoubleArgOptionDefinition(Some(shortopt), longopt, defaultValueName, description,
      { (d: Double, _) => action(d) }))

  def doubleOpt(longopt: String, description: String, action: Double => Unit) =
    add(new DoubleArgOptionDefinition(None, longopt, defaultValueName, description,
      { (d: Double, _) => action(d) }))
      
  def doubleOpt(shortopt: String, longopt: String, valueName: String,
      description: String, action: Double => Unit) =
    add(new DoubleArgOptionDefinition(Some(shortopt), longopt, valueName, description,
      { (d: Double, _) => action(d) }))

  def doubleOpt(shortopt: Option[String], longopt: String, valueName: String,
      description: String, action: Double => Unit) =
    add(new DoubleArgOptionDefinition(shortopt, longopt, valueName, description,
      { (d: Double, _) => action(d) }))
    
  def booleanOpt(shortopt: String, longopt: String, description: String, action: Boolean => Unit) =
    add(new BooleanArgOptionDefinition(Some(shortopt), longopt, defaultValueName, description,
      { (b: Boolean, _) => action(b) }))

  def booleanOpt(longopt: String, description: String, action: Boolean => Unit) =
    add(new BooleanArgOptionDefinition(None, longopt, defaultValueName, description,
      { (b: Boolean, _) => action(b) }))
  
  def booleanOpt(shortopt: String, longopt: String, valueName: String,
      description: String, action: Boolean => Unit) =
    add(new BooleanArgOptionDefinition(Some(shortopt), longopt, valueName, description,
      { (b: Boolean, _) => action(b) }))

  def booleanOpt(shortopt: Option[String], longopt: String, valueName: String,
      description: String, action: Boolean => Unit) =
    add(new BooleanArgOptionDefinition(shortopt, longopt, valueName, description,
      { (b: Boolean, _) => action(b) }))
      
  def keyValueOpt(shortopt: String, longopt: String, description: String, action: (String, String) => Unit) =
    add(new KeyValueArgOptionDefinition(Some(shortopt), longopt, defaultKeyName, defaultValueName, description,
      { (k: String, v: String, _) => action(k, v) }))

  def keyValueOpt(longopt: String, description: String, action: (String, String) => Unit) =
    add(new KeyValueArgOptionDefinition(None, longopt, defaultKeyName, defaultValueName, description,
      { (k: String, v: String, _) => action(k, v) }))
  
  def keyValueOpt(shortopt: String, longopt: String, keyName: String, valueName: String,
      description: String, action: (String, String) => Unit) =
    add(new KeyValueArgOptionDefinition(Some(shortopt), longopt, keyName, valueName, description,
      { (k: String, v: String, _) => action(k, v) }))

  def keyValueOpt(shortopt: Option[String], longopt: String, keyName: String, valueName: String,
      description: String, action: (String, String) => Unit) =
    add(new KeyValueArgOptionDefinition(shortopt, longopt, keyName, valueName, description,
      { (k: String, v: String, _) => action(k, v) }))
  
  def keyIntValueOpt(shortopt: String, longopt: String, description: String, action: (String, Int) => Unit) =
    add(new KeyIntValueArgOptionDefinition(Some(shortopt), longopt, defaultKeyName, defaultValueName, description,
      { (k: String, v: Int, _) => action(k, v) }))

  def keyIntValueOpt(longopt: String, description: String, action: (String, Int) => Unit) =
    add(new KeyIntValueArgOptionDefinition(None, longopt, defaultKeyName, defaultValueName, description,
      { (k: String, v: Int, _) => action(k, v) }))
  
  def keyIntValueOpt(shortopt: String, longopt: String, keyName: String, valueName: String,
      description: String, action: (String, Int) => Unit) =
    add(new KeyIntValueArgOptionDefinition(Some(shortopt), longopt, keyName, valueName, description,
      { (k: String, v: Int, _) => action(k, v) }))

  def keyIntValueOpt(shortopt: Option[String], longopt: String, keyName: String, valueName: String,
      description: String, action: (String, Int) => Unit) =
    add(new KeyIntValueArgOptionDefinition(shortopt, longopt, keyName, valueName, description,
      { (k: String, v: Int, _) => action(k, v) }))
  
  def keyDoubleValueOpt(shortopt: String, longopt: String, description: String, action: (String, Double) => Unit) =
    add(new KeyDoubleValueArgOptionDefinition(Some(shortopt), longopt, defaultKeyName, defaultValueName, description,
      { (k: String, v: Double, _) => action(k, v) }))

  def keyDoubleValueOpt(longopt: String, description: String, action: (String, Double) => Unit) =
    add(new KeyDoubleValueArgOptionDefinition(None, longopt, defaultKeyName, defaultValueName, description,
      { (k: String, v: Double, _) => action(k, v) }))
    
  def keyDoubleValueOpt(shortopt: String, longopt: String, keyName: String, valueName: String,
      description: String, action: (String, Double) => Unit) =
    add(new KeyDoubleValueArgOptionDefinition(Some(shortopt), longopt, keyName, valueName, description,
      { (k: String, v: Double, _) => action(k, v) }))

  def keyDoubleValueOpt(shortopt: Option[String], longopt: String, keyName: String, valueName: String,
      description: String, action: (String, Double) => Unit) =
    add(new KeyDoubleValueArgOptionDefinition(shortopt, longopt, keyName, valueName, description,
      { (k: String, v: Double, _) => action(k, v) }))

  def keyBooleanValueOpt(shortopt: String, longopt: String, description: String, action: (String, Boolean) => Unit) =
    add(new KeyBooleanValueArgOptionDefinition(Some(shortopt), longopt, defaultKeyName, defaultValueName, description,
      { (k: String, v: Boolean, _) => action(k, v) }))

  def keyBooleanValueOpt(longopt: String, description: String, action: (String, Boolean) => Unit) =
    add(new KeyBooleanValueArgOptionDefinition(None, longopt, defaultKeyName, defaultValueName, description,
      { (k: String, v: Boolean, _) => action(k, v) }))

  def keyBooleanValueOpt(shortopt: String, longopt: String, keyName: String, valueName: String,
      description: String, action: (String, Boolean) => Unit) =
    add(new KeyBooleanValueArgOptionDefinition(Some(shortopt), longopt, keyName, valueName, description,
      { (k: String, v: Boolean, _) => action(k, v) }))

  def keyBooleanValueOpt(shortopt: Option[String], longopt: String, keyName: String, valueName: String,
      description: String, action: (String, Boolean) => Unit) =
    add(new KeyBooleanValueArgOptionDefinition(shortopt, longopt, keyName, valueName, description,
      { (k: String, v: Boolean, _) => action(k, v) }))
  
  def help(shortopt: String, longopt: String, description: String) =
    add(new FlagOptionDefinition(Some(shortopt), longopt, description, {_ => this.showUsage; exit}))

  def help(shortopt: Option[String], longopt: String, description: String) =
    add(new FlagOptionDefinition(shortopt, longopt, description, {_ => this.showUsage; exit}))
  
  def separator(description: String) =
    add(new SeparatorDefinition(description))
  
  /** adds an argument invoked by an option without `-` or `--`.
   * @param name name in the usage text
   * @param description description in the usage text
   * @param action callback function
   */  
  def arg(name: String, description: String, action: String => Unit) =
    add(new Argument[Unit](name, description, 1, 1, { (s: String, _) => action(s) }))

  /** adds an optional argument invoked by an option without `-` or `--`.
   * @param name name in the usage text
   * @param description description in the usage text
   * @param action callback function
   */  
  def argOpt(name: String, description: String, action: String => Unit) =
    add(new Argument(name, description, 0, 1,
      { (s: String, _) => action(s) }))
      
  /** adds a list of arguments invoked by options without `-` or `--`.
   * @param name name in the usage text
   * @param description description in the usage text
   * @param action callback function
   */
  def arglist(name: String, description: String, action: String => Unit) =
    add(new Argument(name, description, 1, UNBOUNDED,
      { (s: String, _) => action(s) }))

  /** adds an optional list of arguments invoked by options without `-` or `--`.
   * @param name name in the usage text
   * @param description description in the usage text
   * @param action callback function
   */
  def arglistOpt(name: String, description: String, action: String => Unit) =
    add(new Argument(name, description, 0, UNBOUNDED,
      { (s: String, _) => action(s) }))
}
