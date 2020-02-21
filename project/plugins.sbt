resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.3.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.3")

ivyXML :=
  <dependency org="org.eclipse.jetty.orbit" name="javax.servlet"
  rev="2.5.0.v201103041518">
    <artifact name="javax.servlet" type="orbit" ext="jar"/>
  </dependency>

val scalaJSVersion =
  Option(System.getenv("SCALAJS_VERSION")).getOrElse("1.0.0")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion)
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.6.1")
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "0.6.1")
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.3.9")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.2")
addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "1.5.1")
