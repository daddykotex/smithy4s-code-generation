package smithy4s_codegen.smithy

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.implicits._
import fs2.io.file.{Path => FPath}
import smithy4s_codegen.SmithyClasspathConfig
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.loader.ModelAssembler
import software.amazon.smithy.model.loader.ModelDiscovery
import software.amazon.smithy.model.loader.ModelManifestException
import software.amazon.smithy.model.validation.ValidationEvent

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import scala.jdk.CollectionConverters._

class ModelLoader(allImports: Map[String, List[java.net.URL]]) {
  def load(
      dependencies: List[String],
      content: String
  ): Either[List[ValidationEvent], Model] = {
    val depImports =
      dependencies.flatMap(dep => allImports.get(dep).toList.flatten)
    val res = Model
      .assembler()
      .addUnparsedModel("ui.smithy", content)
      .addImports(depImports)
      .assemble()
    if (res.isBroken()) Left(res.getValidationEvents().asScala.toList)
    else Right(res.unwrap())
  }

  private implicit class ModelAssemblerOps(assembler: ModelAssembler) {
    def addImports(urls: Seq[java.net.URL]): ModelAssembler = {
      urls.foreach(assembler.addImport)
      assembler
    }
  }
}

object ModelLoader {
  def apply(smithyClasspathConfig: SmithyClasspathConfig): IO[ModelLoader] = {
    loadJars(smithyClasspathConfig).map(new ModelLoader(_))
  }

  private def smithyUrls(jar: FPath): IO[List[java.net.URL]] = {
    Resource
      .fromAutoCloseable(
        IO.delay(
          FileSystems.newFileSystem(jar.toNioPath)
        )
      )
      .use { jarFS =>
        val p = jarFS.getPath("META-INF", "smithy", "manifest")

        // model discovery would throw if we tried to pass a non-existent path
        if (!Files.exists(p)) IO.pure(Nil)
        else {
          try {
            IO.delay(
              ModelDiscovery.findModels(p.toUri().toURL()).asScala.toList
            )
          } catch {
            case e: ModelManifestException =>
              IO.delay(
                System.err.println(
                  s"Unexpected exception while loading model from $jar, skipping: $e"
                )
              ).as(Nil)
          }
        }
      }
  }

  private def loadJars(
      smithyClasspathConfig: SmithyClasspathConfig
  ): IO[Map[String, List[java.net.URL]]] = {
    smithyClasspathConfig.entries.toList
      .traverse { case (coordinate, jar) =>
        smithyUrls(jar).tupleLeft(coordinate)
      }
      .map(_.toMap)
  }
}
