name := "scopt"

version := "2.1.0-SNAPSHOT"

organization := "com.github.scopt"

homepage := Some(url("https://github.com/scopt/scopt"))

licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.php"))

description := """a command line options parsing library"""

scalaVersion := "2.9.2"

crossScalaVersions := Seq("2.9.2", "2.9.1", "2.9.0-1", "2.8.1")

// junit
libraryDependencies += "junit" % "junit" % "4.7" % "test"

// scalatest
libraryDependencies <<= (scalaVersion, libraryDependencies) { (sv, deps) =>
	val versionMap = Map("2.8.1" -> "1.5.1", "2.9.0-1" -> "1.6.1", "2.9.1" -> "1.6.1", "2.9.2" -> "1.7.2")
	val libName =
	  if (List("2.8.1") contains sv) "scalatest_2.8.1" 
	  else "scalatest_2.9.0"
	val testVersion = versionMap.getOrElse(sv, error("Unsupported Scala version " + sv))
	deps :+ ("org.scalatest" % libName % testVersion % "test")
}

seq(lsSettings :_*)

LsKeys.tags in LsKeys.lsync := Seq("cli", "command-line", "parsing", "parser")

externalResolvers in LsKeys.lsync := Seq(
  "sonatype-public" at "https://oss.sonatype.org/content/repositories/public")

// scaladoc fix
unmanagedClasspath in Compile += Attributed.blank(new java.io.File("doesnotexist"))

pomExtra :=
  (<scm>
    <url>git@github.com:scopt/scopt.git</url>
    <connection>scm:git:git@github.com:scopt/scopt.git</connection>
  </scm>
  <developers>
    <developer>
      <id>eed3si9n</id>
      <name>Eugene Yokota</name>
      <url>http://eed3si9n.com</url>
    </developer>
  </developers>)

// --- Sonatype settings ---

publishMavenStyle := true

publishArtifact in (Compile, packageBin) := true

publishArtifact in Test := false

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT")) 
    Some("snapshots" at nexus + "content/repositories/snapshots") 
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := { x => false }
