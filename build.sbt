import play.PlayImport.PlayKeys._

name := """las-ws"""

version := "1.1"

scalaVersion := "2.11.12"

javaOptions in Universal += "-J-Xmx4G"

lazy val root = (project in file(".")).enablePlugins(
  PlayScala,
  SystemdPlugin,
  DockerPlugin,
  AshScriptPlugin)

maintainer := "Eetu Mäkelä <eetu.makela@helsinki.fi>"

packageSummary := "las-ws"

packageDescription := "Language analysis web service"

sources in (Compile, doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false

dockerBaseImage := "openjdk:alpine"

dockerExposedPorts in Docker := Seq(9000, 9443)

dockerUpdateLatest := true

dockerUsername := Some("jiemakel")

libraryDependencies ++= Seq(
    "org.webjars" %% "webjars-play" % "2.3.0",
    "org.webjars" % "bootstrap" % "3.0.0",
    "org.webjars" % "underscorejs" % "1.5.2",
    "org.webjars" % "angularjs" % "1.2.0-rc.3",
    "org.webjars" % "angular-ui-router" % "0.2.0",
    "com.softwaremill.macwire" %% "macros" % "0.8.0",
    "fi.seco" % "lexicalanalysis" % "1.5.14",
    "fi.seco" % "lexicalanalysis-resources-fi-complete" % "1.5.14",
    "fi.seco" % "lexicalanalysis-resources-other" % "1.5.14",
    "com.optimaize.languagedetector" % "language-detector" % "0.5",
    //"com.cybozu.labs" % "langdetect" % "1.2.2" exclude("net.arnx.jsonic", "jsonic"),
    //"net.arnx" % "jsonic" % "1.3.0", //langdetect pulls in ancient unavailable version
    //"org.mockito" % "mockito-core" % "1.9.5" % "test",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"
)

resolvers ++= Seq(
    Resolver.mavenLocal,
    "Github Imagination" at "https://github.com/Imaginatio/Maven-repository/raw/master"
)

routesImport ++= Seq("binders.Binders._","java.util.Locale")

