resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.3.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.2")

ivyXML :=
  <dependency org="org.eclipse.jetty.orbit" name="javax.servlet"
  rev="2.5.0.v201103041518">
    <artifact name="javax.servlet" type="orbit" ext="jar"/>
  </dependency>

addSbtPlugin("org.scala-js"       % "sbt-scalajs"                   % "0.6.28")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject"      % "0.6.0")
addSbtPlugin("org.portable-scala" % "sbt-scala-native-crossproject" % "0.6.0")
addSbtPlugin("org.scala-native"   % "sbt-scala-native"              % "0.3.8")
addSbtPlugin("com.jsuereth"       % "sbt-pgp"                       % "1.1.2")
addSbtPlugin("com.eed3si9n"       % "sbt-sriracha"                  % "0.1.0")
