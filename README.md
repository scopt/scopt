  [1]: https://scopt.github.io/scopt/4.0.0/api/scopt/OParserBuilder.html

# scopt
[![Maven Central](https://img.shields.io/maven-central/v/com.github.scopt/scopt_2.11.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.scopt/scopt_2.11)
[![Javadocs](https://javadoc.io/badge/com.github.scopt/scopt_2.12.svg)](https://javadoc.io/doc/com.github.scopt/scopt_2.12)
[![Build Status](https://github.com/scopt/scopt/actions/workflows/ci.yml/badge.svg?branch=develop&event=push)](https://github.com/scopt/scopt/actions/workflows/ci.yml)

scopt is a little command line options parsing library.

Sonatype
--------

```scala
libraryDependencies += "com.github.scopt" %% "scopt" % "X.Y.Z"
```

See the Maven Central badge above.

| Scala Version | JVM | JS (1.x) |  JS (0.6.x) |  Native (0.4.x) |  Native (0.3.x) |
| ------------- | :-: | :------: | :---------: | :------------:  | :------------:  |
| 3.x           | ✅  |   ✅     |     n/a     |      ✅         |      n/a        |
| 2.13.x        | ✅  |   ✅     |     ✅      |      ✅         |      n/a        |
| 2.12.x        | ✅  |   ✅     |     ✅      |      ✅         |      n/a        |
| 2.11.x        | ✅  |   ✅     |     ✅      |      ✅         |      ✅         |

Usage
-----

scopt 4.x provides two styles of constructing a command line option parser: functional DSL and object-oriented DSL.
Either case, first you need a case class that represents the configuration:

```scala
import java.io.File
case class Config(
    foo: Int = -1,
    out: File = new File("."),
    xyz: Boolean = false,
    libName: String = "",
    maxCount: Int = -1,
    verbose: Boolean = false,
    debug: Boolean = false,
    mode: String = "",
    files: Seq[File] = Seq(),
    keepalive: Boolean = false,
    jars: Seq[File] = Seq(),
    kwargs: Map[String, String] = Map())
```

During the parsing process, a config object is passed around as an argument into `action` callbacks.

### Functional DSL

Here's how you create a `scopt.OParser[Config]`.

```scala
import scopt.OParser
val builder = OParser.builder[Config]
val parser1 = {
  import builder._
  OParser.sequence(
    programName("scopt"),
    head("scopt", "4.x"),
    // option -f, --foo
    opt[Int]('f', "foo")
      .action((x, c) => c.copy(foo = x))
      .text("foo is an integer property"),
    // more options here...
  )
}

// OParser.parse returns Option[Config]
OParser.parse(parser1, args, Config()) match {
  case Some(config) =>
    // do something
  case _ =>
    // arguments are bad, error message will have been displayed
}
```

See [Scaladoc API][1] and the rest of this page for the details on various builder methods.

#### Abstracting over effects

By default, scopt emits output when needed to stderr and stdout.  This is expected behavior when using scopt to process arguments for your stand-alone application.  However, if your application requires parsing arguments while not producing output directly, you may wish to intercept the side effects.
Use `OParser.runParser(...)` to do so:

```scala
// OParser.runParser returns (Option[Config], List[OEffect])
OParser.runParser(parser1, args, Config()) match {
  case (result, effects) =>
    OParser.runEffects(effects, new DefaultOEffectSetup {
      // override def displayToOut(msg: String): Unit = Console.out.println(msg)
      // override def displayToErr(msg: String): Unit = Console.err.println(msg)
      // override def reportError(msg: String): Unit = displayToErr("Error: " + msg)
      // override def reportWarning(msg: String): Unit = displayToErr("Warning: " + msg)
      
      // ignore terminate
      override def terminate(exitState: Either[String, Unit]): Unit = ()
    })

    result match {
      Some(config) =>
        // do something
      case _ =>
        // arguments are bad, error message will have been displayed
    }
}
```

#### Full example

```scala
import scopt.OParser
val builder = OParser.builder[Config]
val parser1 = {
  import builder._
  OParser.sequence(
    programName("scopt"),
    head("scopt", "4.x"),
    opt[Int]('f', "foo")
      .action((x, c) => c.copy(foo = x))
      .text("foo is an integer property"),
    opt[File]('o', "out")
      .required()
      .valueName("<file>")
      .action((x, c) => c.copy(out = x))
      .text("out is a required file property"),
    opt[(String, Int)]("max")
      .action({ case ((k, v), c) => c.copy(libName = k, maxCount = v) })
      .validate(x =>
        if (x._2 > 0) success
        else failure("Value <max> must be >0"))
      .keyValueName("<libname>", "<max>")
      .text("maximum count for <libname>"),
    opt[Seq[File]]('j', "jars")
      .valueName("<jar1>,<jar2>...")
      .action((x, c) => c.copy(jars = x))
      .text("jars to include"),
    opt[Map[String, String]]("kwargs")
      .valueName("k1=v1,k2=v2...")
      .action((x, c) => c.copy(kwargs = x))
      .text("other arguments"),
    opt[Unit]("verbose")
      .action((_, c) => c.copy(verbose = true))
      .text("verbose is a flag"),
    opt[Unit]("debug")
      .hidden()
      .action((_, c) => c.copy(debug = true))
      .text("this option is hidden in the usage text"),
    help("help").text("prints this usage text"),
    arg[File]("<file>...")
      .unbounded()
      .optional()
      .action((x, c) => c.copy(files = c.files :+ x))
      .text("optional unbounded args"),
    note("some notes." + sys.props("line.separator")),
    cmd("update")
      .action((_, c) => c.copy(mode = "update"))
      .text("update is a command.")
      .children(
        opt[Unit]("not-keepalive")
          .abbr("nk")
          .action((_, c) => c.copy(keepalive = false))
          .text("disable keepalive"),
        opt[Boolean]("xyz")
          .action((x, c) => c.copy(xyz = x))
          .text("xyz is a boolean property"),
        opt[Unit]("debug-update")
          .hidden()
          .action((_, c) => c.copy(debug = true))
          .text("this option is hidden in the usage text"),
        checkConfig(
          c =>
            if (c.keepalive && c.xyz) failure("xyz cannot keep alive")
            else success)
      )
  )
}

// OParser.parse returns Option[Config]
OParser.parse(parser1, args, Config()) match {
  case Some(config) =>
    // do something
  case _ =>
    // arguments are bad, error message will have been displayed
}
```

The above generates the following usage text:

```
scopt 4.x
Usage: scopt [update] [options] [<file>...]

  -f, --foo <value>        foo is an integer property
  -o, --out <file>         out is a required file property
  --max:<libname>=<max>    maximum count for <libname>
  -j, --jars <jar1>,<jar2>...
                           jars to include
  --kwargs k1=v1,k2=v2...  other arguments
  --verbose                verbose is a flag
  --help                   prints this usage text
  <file>...                optional unbounded args
some notes.

Command: update [options]
update is a command.
  -nk, --not-keepalive     disable keepalive
  --xyz <value>            xyz is a boolean property
```

#### Options

Command line options are defined using `opt[A]('f', "foo")` or `opt[A]("foo")` where `A` is any type that is an instance of `Read` typeclass.

- `Unit` works as a plain flag `--foo` or `-f`
- `Int`, `Long`, `Double`, `String`, `BigInt`, `BigDecimal`, `java.io.File`, `java.nio.file.Path`, `java.net.URI`, and `java.net.InetAddress` accept a value like `--foo 80` or `--foo:80`
- `Boolean` accepts a value like `--foo true` or `--foo:1`
- `java.util.Calendar` accepts a value like `--foo 2000-12-01`
- `scala.concurrent.duration.Duration` accepts a value like `--foo 30s`
- A pair of types like `(String, Int)` accept a key-value like `--foo:k=1` or `-f k=1`
- A `Seq[File]` accepts a string containing comma-separated values such as `--jars foo.jar,bar.jar`
- A `SpaceSep[File]` accepts consecutive space-separated values such as `--jars foo.jar bar.jar`. Parsing stops at the next option, command, or `--`. If the parser also has positional `arg`s, separate them with `--` (for example `--jars foo.jar bar.jar -- file.txt`)
- A `Map[String, String]` accepts a string containing comma-separated pairs like `--kwargs key1=val1,key2=val2`

This could be extended by defining `Read` instances in the scope. For example,

```scala
object WeekDays extends Enumeration {
  type WeekDays = Value
  val Mon, Tue, Wed, Thur, Fri, Sat, Sun = Value
}
implicit val weekDaysRead: scopt.Read[WeekDays.Value] =
  scopt.Read.reads(WeekDays withName _)
```

By default these options are optional.

#### Short options

For plain flags (`opt[Unit]`) short options can be grouped as `-fb` to mean `--foo --bar`.

`opt` accepts only a single character, but using `abbr("ab")` a string can be used too:

```scala
opt[Unit]("no-keepalive").abbr("nk").action( (x, c) => c.copy(keepalive = false) )
```

#### Help, Version, and Notes

There are special options with predefined action called `help("help")` and `version("version")`, which prints usage text and header text respectively. When `help("help")` is defined, parser will print out short error message when it fails instead of printing the entire usage text.

`note("...")` is used add given string to the usage text.

#### Arguments

Command line arguments are defined using `arg[A]("<file>")`. It works similar to options, but instead it accepts values without `--` or `-`. By default, arguments accept a single value and are required.

```scala
arg[String]("<file>...")
```

#### Occurrence

Each opt/arg carries occurrence information `minOccurs` and `maxOccurs`.
`minOccurs` specify at least how many times an opt/arg must appear, and
`maxOccurs` specify at most how many times an opt/arg may appear.

Occurrence can be set using the methods on the opt/arg:

```scala
opt[String]('o', "out").required()
opt[String]('o', "out").required().withFallback(() => "default value")
opt[String]('o', "out").minOccurs(1) // same as above
arg[String]("<mode>").optional()
arg[String]("<mode>").minOccurs(0) // same as above
arg[String]("<file>...").optional().unbounded()
arg[String]("<file>...").minOccurs(0).maxOccurs(1024) // same as above
```

#### Visibility

Each opt/arg can be hidden from the usage text using `hidden()` method:

```scala
opt[Unit]("debug")
  .hidden()
  .action( (_, c) => c.copy(debug = true) )
  .text("this option is hidden in the usage text")
```

#### Validation

Each opt/arg can carry multiple validation functions.

```scala
opt[Int]('f', "foo")
  .action( (x, c) => c.copy(intValue = x) )
  .validate( x =>
    if (x > 0) success
    else failure("Option --foo must be >0") )
  .validate( x => failure("Just because") )
```

The first function validates if the values are positive, and
the second function always fails.

#### Check configuration

Consistency among the option values can be checked using `checkConfig`.

```scala
checkConfig( c =>
  if (c.keepalive && c.xyz) failure("xyz cannot keep alive")
  else success )
```

These are called at the end of parsing.

#### Commands

Commands may be defined using `cmd("update")`. Commands could be used to express `git branch` kind of argument, whose name means something. Using `children` method, a command may define child opts/args that get inserted in the presence of the command. To distinguish commands from arguments, they must appear in the first position within the level. It is generally recommended to avoid mixing args both in parent level and commands to avoid confusion.

```scala
cmd("update")
  .action( (_, c) => c.copy(mode = "update") )
  .text("update is a command.")
  .children(
    opt[Unit]("not-keepalive").abbr("nk").action( (_, c) =>
      c.copy(keepalive = false) ).text("disable keepalive"),
    opt[Boolean]("xyz").action( (x, c) =>
      c.copy(xyz = x) ).text("xyz is a boolean property"),
    checkConfig( c =>
      if (c.keepalive && c.xyz) failure("xyz cannot keep alive")
      else success )
  )
```

In the above, `update test.txt` would trigger the update command, but `test.txt update` won't.

Commands could be nested into another command as follows:

```scala
cmd("backend")
  .text("commands to manipulate backends:\n")
  .action( (x, c) => c.copy(flag = true) )
  .children(
    cmd("update").children(
      arg[String]("<a>").action( (x, c) => c.copy(a = x) )
    )
  )
```

### Object-oriented DSL, immutable parsing

Here's the object-oriented DSL that's mostly source-compatible with scopt 3.x.

Create a parser by extending `scopt.OptionParser[Config]`. See [Scaladoc API][1] for the details on various builder methods.

```scala
val parser = new scopt.OptionParser[Config]("scopt") {
  head("scopt", "4.x")

  opt[Int]('f', "foo")
    .action((x, c) => c.copy(foo = x))
    .text("foo is an integer property")

  opt[File]('o', "out")
    .required()
    .valueName("<file>")
    .action((x, c) => c.copy(out = x))
    .text("out is a required file property")
}

// parser.parse returns Option[C]
parser.parse(args, Config()) match {
  case Some(config) =>
    // do stuff

  case None =>
    // arguments are bad, error message will have been displayed
}
```

### Object-oriented DSL, mutable parsing

Create a `scopt.OptionParser[Unit]` and customize it with the options you need, passing in functions to process each option or argument. Use `foreach` instead of `action`.

Mutable parsing has changed from scopt 3.x. When upgrading from 3.x, replace `parser.parse(args)` with `parser.parse(args, ()).isDefined`.

```scala
val parser = new scopt.OptionParser[Unit]("scopt") {
  head("scopt", "4.x")

  opt[Int]('f', "foo")
    .foreach( x => c = c.copy(foo = x) )
    .text("foo is an integer property")

  opt[File]('o', "out")
    .required()
    .valueName("<file>")
    .foreach( x => c = c.copy(out = x) )
    .text("out is a required file property")
}
if (parser.parse(args, ()).isDefined) {
  // do stuff
}
else {
  // arguments are bad, usage message will have been displayed
}
```

### Advanced: showUsageOnError

When `help("help")` is defined, parser will print out short error message when it fails instead of printing the entire usage text.

This behavior could be changed by overriding `showUsageOnError` as follows:

```scala
import scopt.{ OParserSetup, DefaultOParserSetup }
val setup: OParserSetup = new DefaultOParserSetup {
  override def showUsageOnError = Some(true)
}
val result = OParser.parse(parser1, args, Config(), setup)
```

### Advanced: Rendering mode

scopt 3.5.0 introduced rendering mode, and adopted two-column rendering of the usage text by default. To switch back to the older one-column rendering override the `renderingMode` method:

```scala
import scopt.{ OParserSetup, DefaultOParserSetup }
val setup: OParserSetup = new DefaultOParserSetup {
  override def renderingMode = scopt.RenderingMode.OneColumn
}
val result = OParser.parse(parser1, args, Config(), setup)
```

### Advanced: Termination handling

By default, when the `--help` or `--version` are invoked, they call `sys.exit(0)` after printing the help or version information. If this is not desired (e.g. testing purposes), you can override the `terminate(exitState: Either[String, Unit])` method:

```scala
import scopt.{ OParser, DefaultOEffectSetup }

OParser.runParser(parser1, args, Config()) match {
  case (result, effects) =>
    OParser.runEffects(effects, new DefaultOEffectSetup {
      // ignore terminate
      override def terminate(exitState: Either[String, Unit]): Unit = ()
    })

    result match {
      Some(config) =>
        // do something
      case _ =>
        // arguments are bad, error message will have been displayed
    }
}
```

Building
--------

sbt to build scopt.

License
-------

[MIT License](LICENSE.md).

Credits
-------

- January 13, 2008: Aaron Harnly creates [aaronharnly/scala-options](https://github.com/aaronharnly/scala-options).
- December 1, 2009: Tim Perrett introduces it [as a gist](http://gist.github.com/246481) on [Parsing command lines argument in a "scalaesque" way](http://www.scala-lang.org/node/4380).
- January 10, 2010: James Strachan takes the code, adds usage text, sbt build, etc and creates [jstrachan/scopt](https://github.com/jstrachan/scopt), which is also mentioned in [Scala CLI Library?](http://www.scala-lang.org/node/4959).
- March 4th, 2010: Eugene Yokota joins scopt project, improves usage text, and adds support for key=value option and argument list.
- May 27, 2011: scopt 1.0.0 is released to scala-tools.org.
- March 18, 2012: Eugene adds immutable parser, forks the project to [scopt/scopt](https://github.com/scopt/scopt), and releases scopt 2.0.0.
- June 7, 2013: Eugene rewrites scopt from scratch for polymorphic options, and releases scopt 3.0.0.

Changes
-------

See [notes](https://github.com/scopt/scopt/tree/scopt3/notes).
