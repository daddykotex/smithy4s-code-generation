import org.scalajs.linker.interface.ModuleSplitStyle

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"
ThisBuild / scalaVersion := "2.13.10"

val http4sVersion = "0.23.24"
val smithyVersion = "1.41.1"
val circeVersion = "0.14.1"
val cirisVersion = "3.5.0"

lazy val baseUri = settingKey[String](
  """Base URI of the backend, defaults to `""` (empty string)."""
)
lazy val bundleAssets = settingKey[Boolean](
  """Whether or not assets should be bundled in the backend jar"""
)
ThisBuild / bundleAssets := sys.env
  .get("BUNDLE_ASSETS")
  .map(_.toBoolean)
  .getOrElse(false)

lazy val root = (project in file("."))
  .aggregate(api, frontend, backend)

lazy val api = (project in file("modules/api"))

lazy val frontend = (project in file("modules/frontend"))
  .enablePlugins(ScalaJSPlugin, BuildInfoPlugin, Smithy4sCodegenPlugin)
  .dependsOn(api)
  .settings(
    name := "smithy4s-code-generation-frontend",
    cleanFiles ++= {
      val dir = baseDirectory.value
      Seq(dir / "dist", dir / "node_modules")
    },
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(
          ModuleSplitStyle.SmallModulesFor(List("smithy4s_codegen"))
        )
      // .withSourceMap(true) -- enable for source-map-explorer
    },
    /* Depend on the scalajs-dom library.
     * It provides static types for the browser DOM APIs.
     */
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "2.4.0",
      "com.raquo" %%% "laminar" % "16.0.0",
      "com.disneystreaming.smithy4s" %%% "smithy4s-http4s" % smithy4sVersion.value,
      "org.http4s" %%% "http4s-dom" % "0.2.3",
      "org.http4s" %%% "http4s-client" % http4sVersion
    ),
    baseUri := {
      if (bundleAssets.value) ""
      // Vite will proxy this to the backend. See vite.config.js
      else "/api"
    },
    buildInfoKeys := Seq[BuildInfoKey](baseUri),
    buildInfoPackage := "smithy4s_codegen"
  )

lazy val backend = (project in file("modules/backend"))
  .dependsOn(api)
  .enablePlugins(
    Smithy4sCodegenPlugin,
    JavaAppPackaging,
    DockerPlugin
  )
  .settings(
    name := "smithy4s-code-generation-backend",
    fork := true,
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s-swagger" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-codegen" % smithy4sVersion.value,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "is.cir" %% "ciris" % cirisVersion,
      "software.amazon.smithy" % "smithy-model" % smithyVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion
    ),
    Compile / resourceGenerators += Def.task {
      val dir = frontend.base
      val distDir = dir / "dist"

      if (bundleAssets.value) {
        require(distDir.exists(), s"asset directory unavailable: $distDir")
        val generated = for {
          f <- (distDir ** "*").get
          relative <- f.relativeTo(dir)
        } yield f -> s"$relative"
        val target = (Compile / resourceManaged).value
        val toCopy = generated.map { case (f, relPath) =>
          f -> target / relPath
        }
        IO.copy(toCopy)
        toCopy.map(_._2)
      } else {
        Seq.empty
      }
    },
    Docker / dockerExposedPorts := List(9000),
    Docker / packageName := "smithy4s-code-generation",
    Docker / dockerRepository := Some("daddykotex"),
    dockerAliases ++= Seq(
      dockerAlias.value.withTag(sys.env.get("GITHUB_SHA")),
      dockerAlias.value
        .withName("morning-bird-7081")
        .withRegistryHost(Option("registry.fly.io")),
      dockerAlias.value
        .withTag(sys.env.get("GITHUB_SHA"))
        .withName("morning-bird-7081")
        .withRegistryHost(Option("registry.fly.io"))
    ),
    Docker / version := "latest",
    dockerBaseImage := "eclipse-temurin:17.0.6_10-jre"
  )
