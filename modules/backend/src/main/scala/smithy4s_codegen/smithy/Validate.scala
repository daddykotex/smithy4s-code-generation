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

final class Validate(modelLoader: ModelLoader) {
  def validateContent(
      content: String
  ): IO[Either[List[String], Unit]] = {
    modelLoader
      .load(content)
      .leftMap(_.map(_.getMessage()))
      .map(_ => ())
      .pure[IO]
  }
}
