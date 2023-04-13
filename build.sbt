ThisBuild / scalaVersion := "2.13.9"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .aggregate(frontend, backend)

lazy val frontend = (project in file("modules/frontend"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "smithy4s-code-generation-frontend",
    scalaJSUseMainModuleInitializer := true
  )

lazy val backend = (project in file("modules/backend"))
  .enablePlugins(Smithy4sCodegenPlugin, JavaAppPackaging, DockerPlugin)
  .settings(
    name := "smithy4s-code-generation-backend",
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s-swagger" % smithy4sVersion.value,
      "org.http4s" %% "http4s-ember-server" % "0.23.16"
    ),
    Docker / dockerExposedPorts := List(9000),
    Docker / packageName := "morning-bird-7081",
    Docker / dockerRepository := Some("registry.fly.io"),
    Docker / version := "latest",
    dockerBaseImage := "eclipse-temurin:17.0.6_10-jre"
  )
  .dependsOn(frontend)
