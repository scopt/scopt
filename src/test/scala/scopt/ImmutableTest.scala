package immutabletest

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.FunSuite

/**
 * Tests the use of the options parser
 */
case class Config(out: String = "",
  foo: Int = -1,
  bar: String = null,
  xyz: Boolean = false,
  libname: String = null,
  libfile: String = null,
  maxlibname: String = null,
  maxcount: Int = -1,
  whatnot: String = null,
  files: List[String] = Nil,
  verbose: Boolean = false)

@RunWith(classOf[JUnitRunner])
class ImmutableTest extends FunSuite {
  val parser1 = new scopt.immutable.OptionParser[Config]("scopt") { def options = Seq(
    opt("o", "output", "output") { (v: String, c: Config) => c.copy(out = v) },
    arg("<file>", "some argument") { (v: String, c: Config) => c.copy(whatnot = v) },
    intOpt("f", "foo", "foo is an integer property") { (v: Int, c: Config) => c.copy(foo = v) },
    keyValueOpt("l", "lib", "<libname>", "<filename>", "load library <libname>")
      { (key: String, value: String, c: Config) => c.copy(libname = key, libfile = value) },
    keyIntValueOpt(None, "max", "<libname>", "<max>", "maximum count for <libname>")
      { (key: String, value: Int, c: Config) => c.copy(maxlibname = key, maxcount = value) },
    booleanOpt("xyz", "xyz is a boolean property") { (v: Boolean, c: Config) => c.copy(xyz = v) },
    flag("v", "verbose", "verbose is a flag") { _.copy(verbose = true) }
  )}

  test("valid arguments are parsed correctly") {
    validArguments(parser1, Config(whatnot = "blah"), "blah")
    validArguments(parser1, Config(foo = 35, whatnot = "abc"), "-f", "35", "abc")
    validArguments(parser1, Config(foo = 22, out = "beer", whatnot = "drink"), "-o", "beer", "-f", "22", "drink")
    validArguments(parser1, Config(libname = "key", libfile = "value", whatnot = "drink"), "--lib:key=value", "drink")
    validArguments(parser1, Config(maxlibname = "key", maxcount = 5, whatnot = "drink"), "--max:key=5", "drink")
    validArguments(parser1, Config(xyz = true, whatnot = "drink"), "--xyz", "true", "drink")
    validArguments(parser1, Config(verbose = true, whatnot = "drink"), "--verbose", "drink")
  }

  test("invalid arguments fail") {
    invalidArguments(parser1)
    invalidArguments(parser1, "-z", "blah")
    invalidArguments(parser1, "blah", "blah")
    invalidArguments(parser1, "-z", "abc", "blah")
    invalidArguments(parser1, "-f", "22", "-z", "abc", "blah")
    invalidArguments(parser1, "--xyz")
  }

  test("bad numbers fail to parse nicely") {
    invalidArguments(parser1, "-f", "shouldBeNumber", "blah")
  }

  test("bad booleans fail to parse nicely") {
    invalidArguments(parser1, "--xyz", "shouldBeBoolean", "blah")
  }

  def validArguments(parser: scopt.immutable.OptionParser[Config],
      expectedConfig: Config, args: String*) {
    expect(Some(expectedConfig)) {
      parser.parse(args, Config())
    }
  }

  def invalidArguments(parser: scopt.immutable.OptionParser[Config],
      args: String*) {
    expect(None) {
      parser.parse(args, Config())
    }
  }
}
