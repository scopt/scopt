import com.typesafe.sbt.pgp.PgpKeys._

// shadow sbt-scalajs' crossProject and CrossType until Scala.js 1.0.0 is released
import sbtcrossproject.{crossProject, CrossType}

def v: String = "3.6.1"

lazy val root = project.in(file(".")).
  aggregate(scoptJS, scoptJVM, scoptNative).
  settings(
    publish := {},
    publishLocal := {},
    skip in publish := true)

lazy val scopt = (crossProject(JSPlatform, JVMPlatform, NativePlatform) in file(".")).
  settings(
    inThisBuild(Seq(
      version := v,
      organization := "com.github.scopt",
      scalaVersion := "2.12.2",
      crossScalaVersions := Seq("2.11.8", "2.10.6", "2.12.2", "2.13.0-M1"),
      homepage := Some(url("https://github.com/scopt/scopt")),
      licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php"))
    )),
    name := "scopt",
    // site
    // to preview, preview-site
    // to push, ghpages-push-site
    site.settings,
    site.includeScaladoc(s"$v/api"),
    ghpages.settings,
    git.remoteRepo := "git@github.com:scopt/scopt.git",
    description := """a command line options parsing library""",
    libraryDependencies += "org.specs2" %% "specs2-core" % "3.9.1" % "test",
    scalacOptions ++= Seq("-language:existentials"),
    resolvers += "sonatype-public" at "https://oss.sonatype.org/content/repositories/public",
    // scaladoc fix
    unmanagedClasspath in Compile += Attributed.blank(new java.io.File("doesnotexist"))
  ).
  nativeSettings(
    scalaVersion := "2.11.11",
    crossScalaVersions := Nil
  )

lazy val scoptJS = scopt.js
lazy val scoptJVM = scopt.jvm
lazy val scoptNative = scopt.native

lazy val nativeTest = project.in(file("nativeTest")).
  dependsOn(scoptNative).
  enablePlugins(ScalaNativePlugin).
  settings(scalaVersion := "2.11.8")
