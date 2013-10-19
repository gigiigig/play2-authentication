import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName = "Authentication"
  val appVersion = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm,
    "commons-codec" % "commons-codec" % "1.7"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here    
    publishTo <<= version {
      case _ => Some(Resolver.file("Github Pages", Path.userHome / "Workspace" / "IdeaWorkspace" / "gigiigig.github.com" / "releases" asFile))
    }
  )

}
