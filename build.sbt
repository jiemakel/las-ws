import play.Project._

name := """lexicalanalysis-play"""

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
    "org.webjars" %% "webjars-play" % "2.2.0",
    "org.webjars" % "bootstrap" % "3.0.0",
    "org.webjars" % "underscorejs" % "1.5.2",
    "org.webjars" % "angularjs" % "1.2.0-rc.3",
    "org.webjars" % "angular-ui-router" % "0.2.0",
    "com.softwaremill.macwire" %% "macros" % "0.5",
    "fi.seco" % "lexicalanalysis" % "1.0.0",
    "com.cybozu.labs" % "langdetect" % "1.2.2" exclude("net.arnx.jsonic", "jsonic"),
    "net.arnx" % "jsonic" % "1.3.0", //langdetect pulls in ancient unavailable version
    "org.mockito" % "mockito-core" % "1.9.5" % "test",
    "com.typesafe" %% "scalalogging-slf4j" % "1.0.1"
)

resolvers ++= Seq(
    "Local Maven Repository" at Path.userHome.asFile.toURI.toURL + ".m2/repository",
    "Github Imagination" at "https://github.com/Imaginatio/Maven-repository/raw/master"
)

playScalaSettings

routesImport ++= Seq("binders.Binders._","java.util.Locale")

net.virtualvoid.sbt.graph.Plugin.graphSettings
