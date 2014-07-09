name := "Authentication"
  
lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  ws,
  "commons-codec" % "commons-codec" % "1.7"
)

releaseSettings

publishTo := Some(Resolver.file("Github Pages", Path.userHome / "Workspace" / "IdeaWorkspace" / "gigiigig.github.com" / "releases" asFile))
                                            