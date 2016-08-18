// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository
resolvers ++= Seq("Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    "Local Maven Repository" at Path.userHome.asFile.toURI.toURL + ".m2/repository"
)

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.8")

addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.0")

