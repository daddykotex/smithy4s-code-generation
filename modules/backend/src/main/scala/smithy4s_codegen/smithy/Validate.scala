package smithy4s_codegen.smithy

import cats.effect.IO
import cats.implicits._

final class Validate(modelLoader: ModelLoader) {
  def validateContent(
      dependencies: List[String],
      content: String
  ): IO[Either[List[String], Unit]] = {
    modelLoader
      .load(dependencies, content)
      .leftMap(_.map(_.getMessage()))
      .map(_ => ())
      .pure[IO]
  }
}
