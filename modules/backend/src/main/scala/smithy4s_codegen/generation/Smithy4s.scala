package smithy4s_codegen.generation

import smithy4s.codegen.internals.CodegenTrick
import smithy4s_codegen.smithy.ModelLoader
import software.amazon.smithy.model.validation.ValidationEvent

case class CodegenResult(namespace: String, name: String, content: String)

final class Smithy4s(modelLoader: ModelLoader) {
  def generate(
      dependencies: List[String],
      content: String
  ): Either[List[ValidationEvent], List[(os.RelPath, CodegenResult)]] = {
    modelLoader
      .load(dependencies, content)
      .map(model => CodegenTrick.run(model, None, None))
  }
}
