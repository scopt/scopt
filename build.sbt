import sbtcrossproject.{crossProject, CrossType}

enablePlugins(ScalaNativePlugin)

def v: String = "3.5.1"

lazy val root = project.in(file(".")).
  aggregate(scoptJS, scoptJVM, scoptNative).
  settings(
    publish := {},
    publishLocal := {})

val nativeSettings = Seq(
  libraryDependencies -= "org.specs2" %%% "specs2-core" % "3.8.6" % "test",
  publishArtifact in (Compile, packageDoc) := false,
  publishArtifact in packageDoc := false,
  sources in (Compile, doc) := Seq.empty
)


lazy val scopt = (crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .settings(
    inThisBuild(Seq(
      version := v,
      organization := "com.github.scopt",
      scalaVersion := "2.11.11",
      crossScalaVersions := Seq("2.11.8", "2.10.6", "2.12.2"),
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
    libraryDependencies += "org.specs2" %% "specs2-core" % "3.8.6" % "test",
    scalacOptions ++= Seq("-language:existentials"),
    resolvers += "sonatype-public" at "https://oss.sonatype.org/content/repositories/public",
    // scaladoc fix
    unmanagedClasspath in Compile += Attributed.blank(new java.io.File("doesnotexist"))
  )
  .nativeSettings(nativeSettings: _*) in file("."))

lazy val scoptJS = scopt.js
lazy val scoptJVM = scopt.jvm
lazy val scoptNative = scopt.native

lazy val testNative = Project("testNative", file("test-native"))
  .enablePlugins(ScalaNativePlugin)
  .settings(nativeSettings: _*)
  .settings(normalizedName := "scopt-test-native")
  .dependsOn(scoptNative)
