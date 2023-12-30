import org.scalajs.linker.interface.ModuleSplitStyle
import com.typesafe.sbt.packager.docker._
import smithy4s_codegen._

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

lazy val smithyClasspath = settingKey[Seq[ModuleID]](
  """List of artifacts to include in backend image that has dependencies"""
)
lazy val smithyClasspathDir = settingKey[String](
  """Path of the smithy classpath directory (where we mount the config and the jars)"""
)

lazy val root = (project in file("."))
  .aggregate(api, frontend, backend, backendDependencies)

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

lazy val smithyClasspathSettings = Def.settings(
  Universal / mappings ++= {
    val depRes = dependencyResolution.value
    val artifacts = smithyClasspath.value
    val smithyClasspathOutput = target.value / "smithy-classpath"
    val logger = sLog.value
    val resolved = artifacts.flatMap { module =>
      depRes.retrieve(module, None, target.value, logger) match {
        case Left(value) =>
          sys.error(s"Unable to resolve smithy classpath module $module")
        case Right(value) => value.headOption.map(f => module -> f)
      }
    }
    val smithyClasspathValue = smithyClasspathDir.value

    val entries: Seq[SmithyClasspathEntry] =
      resolved.map { case (module, file) =>
        SmithyClasspathEntry(
          module,
          file,
          s"$smithyClasspathValue/${file.name}"
        )
      }
    val entriesMapping =
      entries.map { case SmithyClasspathEntry(_, file, pathInImage) =>
        file -> pathInImage
      }

    val smithyClasspathFile = target.value / "smithy-classpath.json"
    SmithyClasspath.toFile(
      smithyClasspathFile,
      entries,
      (Docker / defaultLinuxInstallLocation).value
    )
    val configMapping = Seq(
      smithyClasspathFile -> s"$smithyClasspathValue/smithy-classpath.json"
    )
    entriesMapping ++ configMapping
  },
  dockerEnvVars ++= {
    val inDockerPath = (Docker / defaultLinuxInstallLocation).value
    val smithyClasspathValue = smithyClasspathDir.value
    Map(
      "SMITHY_CLASSPATH_CONFIG" -> s"$inDockerPath/$smithyClasspathValue/smithy-classpath.json"
    )
  }
)

lazy val backend = (project in file("modules/backend"))
  .dependsOn(api)
  .enablePlugins(
    Smithy4sCodegenPlugin,
    JavaAppPackaging,
    DockerPlugin
  )
  .settings(smithyClasspathSettings)
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
    smithyClasspath := Seq.empty,
    smithyClasspathDir := "smithy-classpath",
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
    dockerAliases ++= {
      val sha = sys.env.get("GITHUB_SHA").map(_.take(10))
      val latests = Seq(
        dockerAlias.value
          .withName("morning-bird-7081")
          .withRegistryHost(Option("registry.fly.io"))
      )
      val shas = sha.toSeq.flatMap { s =>
        Seq(
          dockerAlias.value.withTag(Some(s)),
          dockerAlias.value
            .withTag(Some(s))
            .withName("morning-bird-7081")
            .withRegistryHost(Option("registry.fly.io"))
        )
      }

      latests ++ shas
    },
    Docker / version := "latest",
    dockerBaseImage := "eclipse-temurin:17.0.6_10-jre"
  )

/** This is a project that's only intented to be a copy of backend but that
  * builds in an image with some dependencies for the smithy-classpath.
  */
lazy val backendDependencies = project
  .enablePlugins(DockerPlugin)
  .settings(smithyClasspathSettings)
  .settings(
    smithyClasspath := Seq(
      "com.disneystreaming.alloy" % "alloy-core" % "0.2.8"
    ),
    smithyClasspathDir := "smithy-classpath",
    Docker / packageName := "smithy4s-code-generation",
    Docker / dockerRepository := Some("daddykotex"),
    dockerAliases := {
      val beAlias = (backend / dockerAlias).value
      val sha = sys.env.get("GITHUB_SHA").map(_.take(10))
      val tags =
        sha.map(s => s"with-dependencies-$s").toSeq ++ Seq("with-dependencies")
      tags.flatMap { t =>
        Seq(
          dockerAlias.value.withTag(Some(t)),
          dockerAlias.value
            .withTag(Some(t))
            .withName("morning-bird-7081")
            .withRegistryHost(Option("registry.fly.io"))
        )
      }
    },
    dockerEntrypoint := (backend / dockerEntrypoint).value,
    dockerBaseImage := (backend / dockerAlias).value.toString
  )
