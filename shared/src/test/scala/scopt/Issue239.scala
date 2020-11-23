import minitest.SimpleTestSuite

object Issue239 extends SimpleTestSuite with PowerAssertions {
  test("double arg should accept negative numbers") {
    val set = List("-3.1415926" -> -3.1415926, "-.1" -> -.1)
    set.foreach { case (arg, expected) =>
      val p = new scopt.OptionParser[Double]("Test") {
        opt[String]('n', "name")
        arg[Double]("value").action((v, _) => v)
      }
      val res = p.parse(Array(arg), Double.NaN)
      assert(res == Some(expected))
    }
    ()
  }

  test("int arg should accept negative numbers") {
    val set = List("-3" -> -3, "-0" -> 0)
    set.foreach { case (arg, expected) =>
      val p = new scopt.OptionParser[Int]("Test") {
        opt[String]('n', "name")
        arg[Int]("value").action((v, _) => v)
      }
      val res = p.parse(Array(arg), Int.MaxValue)
      assert(res == Some(expected))
    }
    ()
  }

  test("double opt should accept negative numbers") {
    val set = List("-3.1415926" -> -3.1415926, "-.1" -> -.1)
    set.foreach { case (arg, expected) =>
      val p = new scopt.OptionParser[Double]("Test") {
        opt[Double]('v', "value").action((v, _) => v)
      }
      val res = p.parse(Array("-v", arg), Double.NaN)
      assert(res == Some(expected))
    }
    ()
  }

  test("int opt should accept negative numbers") {
    val set = List("-3" -> -3, "-0" -> 0)
    set.foreach { case (arg, expected) =>
      val p = new scopt.OptionParser[Int]("Test") {
        opt[Int]('v', "value").action((v, _) => v)
      }
      val res = p.parse(Array("--value", arg), Int.MaxValue)
      assert(res == Some(expected))
    }
    ()
  }
}
