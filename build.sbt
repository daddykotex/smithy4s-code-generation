import org.scalajs.linker.interface.ModuleSplitStyle

ThisBuild / scalaVersion := "3.2.2"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

lazy val buildFrontend = taskKey[Seq[(File, String)]](
  "Build the frontend, for production, and return all files generated."
)
lazy val baseUri = settingKey[String](
  """Base URI of the backend, defaults to `""` (empty string)."""
)

lazy val root = (project in file("."))
  .aggregate(frontend, backend)

lazy val commonSettings = Def.settings(
  scalacOptions += "-no-indent"
)

lazy val frontend = (project in file("modules/frontend"))
  .enablePlugins(ScalaJSPlugin, BuildInfoPlugin)
  .settings(commonSettings)
  .settings(
    name := "smithy4s-code-generation-frontend",
    cleanFiles ++= {
      val dir = baseDirectory.value
      Seq(dir / "dist", dir / "node_modules")
    },
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
          ModuleSplitStyle.SmallModulesFor(List("smithy4s_codegen"))
        )
    },
    /* Depend on the scalajs-dom library.
     * It provides static types for the browser DOM APIs.
     */
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "2.4.0",
      "com.raquo" %%% "laminar" % "15.0.0"
    ),
    baseUri := {
      if (insideCI.value) "" else "http://localhost:9000"
    },
    buildInfoKeys := Seq[BuildInfoKey](baseUri),
    buildInfoPackage := "smithy4s_codegen",
    buildFrontend := {
      import sys.process._

      val npmi = Seq("npm", "i")
      val npmBuild = Seq("npm", "run", "build")

      val dir = baseDirectory.value
      val logger = sLog.value

      def runIn(dir: File)(cmd: Seq[String]): Int = {
        Process(cmd, cwd = Some(dir)).!(logger)
      }
      require(runIn(dir)(npmi) == 0, s"[${npmi.mkString(" ")}] failed.")
      require(runIn(dir)(npmBuild) == 0, s"[${npmi.mkString(" ")}] failed.")

      val distDir = dir / "dist"
      for {
        f <- (distDir ** "*").get
        relative <- f.relativeTo(dir)
      } yield f -> s"$relative"
    }
  )

lazy val backend = (project in file("modules/backend"))
  .enablePlugins(
    Smithy4sCodegenPlugin,
    JavaAppPackaging,
    DockerPlugin
  )
  .settings(commonSettings)
  .settings(
    name := "smithy4s-code-generation-backend",
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s-swagger" % smithy4sVersion.value,
      "software.amazon.smithy" % "smithy-model" % "1.30.0",
      "org.http4s" %% "http4s-ember-server" % "0.23.16"
    ),
    Compile / resourceGenerators += Def.task {
      val generated = (frontend / buildFrontend).value
      val target = (Compile / resourceManaged).value
      val toCopy = generated.map { case (f, relPath) => f -> target / relPath }
      IO.copy(toCopy)
      toCopy.map(_._2)
    },
    Docker / dockerExposedPorts := List(9000),
    Docker / packageName := "morning-bird-7081",
    Docker / dockerRepository := Some("registry.fly.io"),
    Docker / version := "latest",
    dockerBaseImage := "eclipse-temurin:17.0.6_10-jre"
  )
