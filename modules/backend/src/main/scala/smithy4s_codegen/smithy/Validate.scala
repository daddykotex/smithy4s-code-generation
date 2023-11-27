package smithy4s_codegen.smithy

import cats.effect.IO
import cats.implicits._

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
