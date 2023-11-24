package smithy4s_codegen.smithy

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.implicits._
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.loader.ModelAssembler
import software.amazon.smithy.model.loader.ModelDiscovery
import software.amazon.smithy.model.loader.ModelManifestException
import software.amazon.smithy.model.validation.ValidationEvent

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import scala.jdk.CollectionConverters._

class ModelLoader(imports: List[java.net.URL]) {
  def load(content: String): Either[List[ValidationEvent], Model] = {
    val res = Model
      .assembler()
      .addUnparsedModel("ui.smithy", content)
      .addImports(imports)
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
  def apply(localJars: List[File]): IO[ModelLoader] = {
    loadJars(localJars).use {
      new ModelLoader(_).pure[IO]
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
