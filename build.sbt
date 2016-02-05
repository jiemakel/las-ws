import play.PlayImport.PlayKeys._

name := """lexicalanalysis-play"""

version := "1.1"

scalaVersion := "2.11.6"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
    "org.webjars" %% "webjars-play" % "2.3.0",
    "org.webjars" % "bootstrap" % "3.0.0",
    "org.webjars" % "underscorejs" % "1.5.2",
    "org.webjars" % "angularjs" % "1.2.0-rc.3",
    "org.webjars" % "angular-ui-router" % "0.2.0",
    "com.softwaremill.macwire" %% "macros" % "0.8.0",
    "fi.seco" % "lexicalanalysis" % "1.1.2",
    "com.cybozu.labs" % "langdetect" % "1.2.2" exclude("net.arnx.jsonic", "jsonic"),
    "net.arnx" % "jsonic" % "1.3.0", //langdetect pulls in ancient unavailable version
    "org.mockito" % "mockito-core" % "1.9.5" % "test",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"
)

resolvers ++= Seq(
    Resolver.mavenLocal,
    "Github Imagination" at "https://github.com/Imaginatio/Maven-repository/raw/master"
)

routesImport ++= Seq("binders.Binders._","java.util.Locale")

net.virtualvoid.sbt.graph.Plugin.graphSettings
