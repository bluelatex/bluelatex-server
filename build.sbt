import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences._

import UnidocKeys._

lazy val commonSettings = Seq(
  resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  organization := "bluelatex",
  version := "2.0.0-SNAPSHOT",
  scalaVersion := "2.11.7",
  scalacOptions in (Compile,doc) ++= Seq("-groups", "-implicits"),
  autoAPIMappings := true,
  scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")) ++ scalariformSettings ++ Seq(
    ScalariformKeys.preferences := {
    ScalariformKeys.preferences.value
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
    })

lazy val bluelatex = project.in(file("."))
  .enablePlugins(JavaServerAppPackaging)
  .settings(commonSettings: _*)
  .settings(unidocSettings: _*)
  .settings(
    name := "bluelatex",
    scalacOptions in (ScalaUnidoc, unidoc) += "-Ymacro-expand:none",
    maintainer in Linux := "The \\BlueLaTeX Teams <blue-dev@lists.gnieh.org>",
    packageSummary in Linux := "\\BlueLaTeX core server",
    packageDescription := """\BlueLaTeX is a server that allows for real-time collaborative document editing.
                            |It provides alle the basic functionalities to manager user accounts, document management,
                            |right management and more.
                            |The server exposes a clean an documented Rest API so that several clients may interact with it.""".stripMargin,
    daemonUser in Linux := normalizedName.value,
    daemonGroup in Linux := (daemonUser in Linux).value)
  .aggregate(core)

lazy val coreDeps = Seq(
  "com.typesafe.akka" %% "akka-http-experimental" % "2.0",
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.0",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.1" % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "org.slf4j" % "jcl-over-slf4j" % "1.7.13",
  "net.ceedubs" %% "ficus" % "1.1.2",
  "com.github.pathikrit" %% "better-files" % "2.13.0",
  "com.lihaoyi" %% "fastparse" % "0.3.3",
  "com.typesafe" % "config" % "1.3.0",
  "com.github.scopt" %% "scopt" % "3.3.0")

lazy val core = project
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "core",
    buildInfoKeys := Seq[BuildInfoKey](
      version,
      scalaVersion,
      BuildInfoKey.action("buildTime") {
        System.currentTimeMillis
      }
    ),
    buildInfoPackage := "bluelatex",
    buildInfoObject := "BlueLaTeXInfo",
    libraryDependencies ++= coreDeps)
