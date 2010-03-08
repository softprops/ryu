import sbt._

class RyuProject(info: ProjectInfo) extends DefaultProject(info) with posterous.Publish {
  // databinder
  val databinderNet = "databinder.net repository" at "http://databinder.net/repo"
  def databind(p:String) = "net.databinder" %% "dispatch-%s".format(p) % "0.7.2"
  val ljs = databind("lift-json")
  val hjs = databind("http-json")
  val js = databind("json")
  val mime = databind("mime")
  
  // lift js
  val liftJson = "net.liftweb" %% "lift-json" % "2.0-M3"
  
  // testing
  val snapshots = "Scala Tools Snapshots" at "http://www.scala-tools.org/repo-snapshots/"
  val specs = "org.scala-tools.testing" % "specs" % "1.6.2.1-SNAPSHOT" % "test"
}