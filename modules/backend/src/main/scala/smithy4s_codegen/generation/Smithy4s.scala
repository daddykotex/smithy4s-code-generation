package smithy4s_codegen.generation
import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.Resource
import cats.effect.syntax.all._
import cats.syntax.all._
import smithy4s.codegen._
import smithy4s.codegen.internals.CodegenTrick
import smithy4s_codegen.smithy.ModelLoader
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.validation.ValidationEvent

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import scala.jdk.CollectionConverters._

case class CodegenResult(namespace: String, name: String, content: String)

final class Smithy4s(modelLoader: ModelLoader) {
  def generate(
      content: String
  ): Either[List[ValidationEvent], List[(os.RelPath, CodegenResult)]] = {
    modelLoader
      .load(content)
      .map(model => CodegenTrick.run(model, None, None))
  }
}
