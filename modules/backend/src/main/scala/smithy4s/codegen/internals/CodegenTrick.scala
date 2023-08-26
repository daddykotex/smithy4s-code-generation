package smithy4s.codegen.internals

import smithy4s_codegen.generation.CodegenResult
import software.amazon.smithy.model.Model

object CodegenTrick {
  def run(
      model: Model,
      allowedNS: Option[Set[String]],
      excludedNS: Option[Set[String]]
  ): List[(os.RelPath, CodegenResult)] =
    CodegenImpl
      .generate(model, allowedNS, excludedNS)
      .map { case (p, r) => p -> CodegenResult(r.namespace, r.name, r.content) }
}
