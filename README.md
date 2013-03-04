scopt
=====

scopt is a little command line options parsing library.

Its based on the code from Tim Perrett which his based on Aaron Harnly's code
mentioned [in this thread](http://old.nabble.com/-scala--CLI-library--ts19391923.html#a19391923) and
[this thread](http://old.nabble.com/Parsing-command-lines-argument-in-a-%22scalaesque%22-way-tp26592006p26595257.html)
which is available [as a gist](http://gist.github.com/246481) or [here](http://harnly.net/tmp/OptionsParser.scala).

Sonatype
--------

```scala
libraryDependencies += "com.github.scopt" %% "scopt" % "2.1.0"

resolvers += "sonatype-public" at "https://oss.sonatype.org/content/groups/public"
```

Usage
-----

scopt provides two flavors of parsers: immutable and mutable.
Either case, first you need a case class that represents the configuration:

```scala
case class Config(foo: Int = -1, bar: String = "", xyz: Boolean = false,
  libname: String = "", libfile: String = "", maxlibname: String = "",
  maxcount: Int = -1, whatnot: String = "")
```

An immutable parser lets you pass around a config object as an argument into callback closures.
On the other hand, the mutable parsers are expected to modify a config object in place.

### Immutable Parser

Here's how you create a `scopt.immutable.OptionParser`. See [Scaladoc API](http://scopt.github.com/scopt/latest/api/) for the details on various builder methods.

```scala
val parser = new scopt.immutable.OptionParser[Config]("scopt", "2.x") { def options = Seq(
  intOpt("f", "foo", "foo is an integer property") { (v: Int, c: Config) => c.copy(foo = v) },
  opt("o", "output", "output") { (v: String, c: Config) => c.copy(bar = v) },
  booleanOpt("xyz", "xyz is a boolean property") { (v: Boolean, c: Config) => c.copy(xyz = v) },
  keyValueOpt("l", "lib", "<libname>", "<filename>", "load library <libname>")
    { (key: String, value: String, c: Config) => c.copy(libname = key, libfile = value) },
  keyIntValueOpt(None, "max", "<libname>", "<max>", "maximum count for <libname>")
    { (key: String, value: Int, c: Config) => c.copy(maxlibname = key, maxcount = value) },
  arg("<file>", "some argument") { (v: String, c: Config) => c.copy(whatnot = v) }
) }
// parser.parse returns Option[C]
parser.parse(args, Config()) map { config =>
  // do stuff
} getOrElse {
  // arguments are bad, usage message will have been displayed
}
```

The above generates the following usage text:

    scopt 2.x
    Usage: scopt [options] <file>

      -f <value> | --foo <value>
            foo is an integer property
      -o <value> | --output <value>
            output
      --xyz <value>
            xyz is a boolean property
      -l:<libname>=<filename> | --lib:<libname>=<filename>
            load library <libname>
      --max:<libname>=<max>
            maximum count for <libname>
      <file>
            some argument

### Mutable Parser

Create a `scopt.mutable.OptionParser` and customise it with the options you need, passing in functions to process each option or argument.

```scala
val parser = new scopt.mutable.OptionParser("scopt", "1.x") {
  intOpt("f", "foo", "foo is an integer property", { v: Int => config.foo = v })
  opt("o", "output", "<file>", "output is a string property", { v: String => config.bar = v })
  booleanOpt("xyz", "xyz is a boolean property", { v: Boolean => config.xyz = v })
  keyValueOpt("l", "lib", "<libname>", "<filename>", "load library <libname>",
    {(key: String, value: String) => { config.libname = key; config.libfile = value } })
  arg("<singlefile>", "<singlefile> is an argument", { v: String => config.whatnot = v })
  // arglist("<file>...", "arglist allows variable number of arguments",
  //   { v: String => config.files = (v :: config.files).reverse })
}
if (parser.parse(args)) {
  // do stuff
}
else {
  // arguments are bad, usage message will have been displayed
}
```

Building
--------

sbt to build scopt.

License
-------

MIT License.

Changes
-------

See [notes](https://github.com/scopt/scopt/tree/master/notes).
