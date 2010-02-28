import sbt._

class RyuProject(info: ProjectInfo) extends DefaultProject(info) {
  override def crossScalaVersions = Set("2.7.6", "2.7.7", "2.8.0.Beta1-RC1") 
  
  // configgy
  val lagNet = "lag.net repository" at "http://www.lag.net/repo"
  // databinder
  val databinderNet = "databinder.net repository" at "http://databinder.net/repo"
  
  // dispatch
  val dpVersion = "0.7.0"
  val dispatchOauth = "net.databinder" %% "dispatch-oauth" % dpVersion
  val dispatchLiftJson = "net.databinder" %% "dispatch-lift-json" % dpVersion
  val dispatchHttpJson = "net.databinder" %% "dispatch-http-json" % dpVersion
  val dispatchJson = "net.databinder" %% "dispatch-json" % dpVersion
  val liftJson = "net.liftweb" %% "lift-json" % "1.1-M8"
  
  // testing
  val configgy = "net.lag" % "configgy" % "1.4" intransitive()
  val snapshots = "Scala Tools Snapshots" at "http://www.scala-tools.org/repo-snapshots/"
  val specs = "org.scala-tools.testing" % "specs" % "1.6.2-SNAPSHOT" % "test"
}