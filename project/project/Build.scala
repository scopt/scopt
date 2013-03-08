import sbt._
object PluginDef extends Build {
  override def projects = Seq(root)
  lazy val root = Project("plugins", file(".")) dependsOn(ghpages, site)
  lazy val ghpages = uri("git://github.com/jsuereth/xsbt-ghpages-plugin.git#1e7611")
  lazy val site = uri("git://github.com/sbt/sbt-site.git#268967")
}
