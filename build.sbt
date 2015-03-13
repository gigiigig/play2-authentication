name := "Authentication"
  
lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  ws,
  "commons-codec" % "commons-codec" % "1.7"
)

releaseSettings

crossScalaVersions := Seq("2.10.4", "2.11.1")

publishTo := Some(Resolver.file("Github Pages", Path.userHome / "Workspace" / "IdeaWorkspace" / "gigiigig.github.com" / "releases" asFile))

// foo
