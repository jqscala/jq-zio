val scala3Version = "3.3.0"
val jqVersion = "0.1.0-SNAPSHOT"
val scalatestVersion = "3.2.9"
val circeVersion = "0.14.1"

lazy val root = project
  .in(file("."))
  .settings(
    organization := "jqscala",
    name := "jq-zio",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % circeVersion,
      "jqscala" %% "jqscala" % jqVersion,
      "jqscala" %% "jqscala" % jqVersion classifier "tests",
      "dev.zio" %% "zio" % "2.1.11",
      "dev.zio" %% "zio-streams" % "2.1.11",
      "dev.zio" %% "zio-http" % "3.0.1",
      "dev.zio" %% "zio-json" % "0.7.3",
      "dev.zio" %% "zio-logging-jul-bridge" % "2.3.2",
      "org.scalatest" %% "scalatest" % scalatestVersion % Test
    )
  )

  