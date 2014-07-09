name := "Authentication"
  
libraryDependencies ++= Seq(
  "commons-codec" % "commons-codec" % "1.7"
)

play.Project.playScalaSettings

releaseSettings

publishTo := Some(Resolver.file("Github Pages", Path.userHome / "Workspace" / "IdeaWorkspace" / "gigiigig.github.com" / "releases" asFile))
                                            