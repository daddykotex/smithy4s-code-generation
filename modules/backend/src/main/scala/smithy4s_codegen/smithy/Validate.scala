package smithy4s_codegen.smithy

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.kernel.Resource
import cats.implicits._
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.loader.ModelAssembler
import software.amazon.smithy.model.loader.ModelDiscovery
import software.amazon.smithy.model.loader.ModelManifestException

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import scala.jdk.CollectionConverters._

final class Validate(imports: List[java.net.URL]) {
  def validateContent(
      content: String
  ): IO[Either[NonEmptyList[String], Unit]] = {
    val res =
      Model
        .assembler()
        .addUnparsedModel("ui.smithy", content)
        .addImports(imports)
        .assemble()
    if (res.isBroken()) {
      val errorList =
        res.getValidationEvents().asScala.toList.map(_.getMessage())
      Left(NonEmptyList.of(errorList.head, errorList.tail: _*)).pure[IO]
    } else {
      Right(()).pure[IO]
    }
  }

  implicit class ModelAssemblerOps(assembler: ModelAssembler) {
    def addImports(urls: Seq[java.net.URL]): ModelAssembler = {
      urls.foreach(assembler.addImport)
      assembler
    }
  }
}

object Validate {
  def apply(localJars: List[File]): IO[Validate] = {
    loadJars(localJars).use {
      new Validate(_).pure[IO]
    }
  }

  private def loadJars(
      localJars: List[File]
  ): Resource[IO, List[java.net.URL]] = {
    localJars
      .traverse { file =>
        Resource
          .fromAutoCloseable(
            IO.delay(
              FileSystems.newFileSystem(file.toPath())
            )
          )
          .map { jarFS =>
            val p = jarFS.getPath("META-INF", "smithy", "manifest")

            // model discovery would throw if we tried to pass a non-existent path
            if (!Files.exists(p)) Nil
            else {
              try ModelDiscovery.findModels(p.toUri().toURL()).asScala.toList
              catch {
                case e: ModelManifestException =>
                  System.err.println(
                    s"Unexpected exception while loading model from $file, skipping: $e"
                  )
                  Nil
              }
            }
          }
      }
      .map(_.flatten)
  }
}
