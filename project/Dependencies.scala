import sbt._
import Keys._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object Dependencies {
  lazy val scala210 = "2.10.7"
  lazy val scala211 = "2.11.12"
  lazy val scala212 = "2.12.8"
  lazy val scala213 = "2.13.0"

  lazy val parserCombinators = Def.setting {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 10 | 13)) =>
        Nil
      case _ =>
        Seq("org.scala-lang.modules" %%% "scala-parser-combinators" % "1.1.0" % Test)
    }
  }
}
