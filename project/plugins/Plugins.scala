import sbt._

class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
  val t_repo = "t_repo" at "http://tristanhunt.com:8081/content/groups/public/"
  val posterous = "net.databinder" % "posterous-sbt" % "0.1.3"
  
  val lessRepo = "lessis repo" at "http://repo.lessis.me"
  val growl = "me.lessis" % "sbt-growl-plugin" % "0.0.4"
}