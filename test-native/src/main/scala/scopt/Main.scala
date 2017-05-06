package scopt

import java.io.File


object Main {
    def main(args: Array[String]): Unit = {

        case class Config(foo: Int = -1, out: File = new File("."), xyz: Boolean = false,
            libName: String = "", maxCount: Int = -1, verbose: Boolean = false, debug: Boolean = false,
            mode: String = "", files: Seq[File] = Seq(), keepalive: Boolean = false,
            jars: Seq[File] = Seq(), kwargs: Map[String,String] = Map())

        val parser = new scopt.OptionParser[Config]("scopt") {
            head("scopt", "3.x")

            opt[Int]('f', "foo").action( (x, c) =>
                c.copy(foo = x) ).text("foo is an integer property")

            note("some notes.")
        }


        // parser.parse returns Option[C]
        parser.parse(args, Config()) match {
            case Some(config) =>
                // do stuff

            case None =>
                // arguments are bad, error message will have been displayed
        }
    }
}
