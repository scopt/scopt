def v: String = "3.6.0"

lazy val scopt = 
  crossProject(JVMPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("."))
  .settings(
    inThisBuild(Seq(
      version := v,
      organization := "com.github.scopt",
      scalaVersion := "2.11.8",
      crossScalaVersions := Seq("2.11.8", "2.10.6", "2.12.0"),
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
  .nativeSettings(resolvers += Resolver.sonatypeRepo("snapshots"))

lazy val scoptJVM = scopt.jvm

lazy val scoptNative = scopt.native

