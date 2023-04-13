import org.scalajs.linker.interface.ModuleSplitStyle

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
    scalaJSUseMainModuleInitializer := true,
    /* Configure Scala.js to emit modules in the optimal way to
     * connect to Vite's incremental reload.
     * - emit ECMAScript modules
     * - emit as many small modules as possible for classes in the "livechart" package
     * - emit as few (large) modules as possible for all other classes
     *   (in particular, for the standard library)
     */
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(
          ModuleSplitStyle.SmallModulesFor(List("com.example.frontend"))
        )
    },
    /* Depend on the scalajs-dom library.
     * It provides static types for the browser DOM APIs.
     */
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.4.0"
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
