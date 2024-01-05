package smithy4s_codegen

import cats.effect.Resource
import cats.effect.IO
import smithy4s_codegen.api.SmithyCodeGenerationService
import com.raquo.airstream.core.EventStream
import smithy4s.http4s.SimpleRestJsonBuilder
import org.http4s.dom.FetchClientBuilder
import org.http4s.Uri
import smithy4s_codegen.BuildInfo.baseUri
import smithy4s.kinds.PolyFunction
import cats.effect.std.Dispatcher
import org.http4s.client.Client
import util.chaining._

object ApiBuilder {
  def build: Resource[IO, SmithyCodeGenerationService[EventStream]] =
    Dispatcher.sequential[IO].flatMap { dispatcher =>
      for {
        ec <- IO.executionContext.toResource
        client <-
          SimpleRestJsonBuilder(SmithyCodeGenerationService)
            .client(FetchClientBuilder[IO].create)
            .uri(Uri.unsafeFromString(baseUri))
            .resource
      } yield {
        client.transform(new PolyFunction[IO, EventStream] {
          def apply[A](fa: IO[A]): EventStream[A] = EventStream
            .fromFuture(dispatcher.unsafeToFuture(fa), emitOnce = true)(ec)
        })
      }

    }
}
