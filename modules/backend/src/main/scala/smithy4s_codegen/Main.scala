package smithy4s_codegen

import cats.effect._
import cats.implicits._
import com.comcast.ip4s._
import org.http4s._
import org.http4s.ember.server._
import org.http4s.implicits._
import smithy4s._
import smithy4s_codegen.api._
import smithy4s.http4s.SimpleRestJsonBuilder
import smithy4s_codegen.smithy.Validate

object SmithyCodeGenerationServiceImpl extends SmithyCodeGenerationService[IO] {
  def healthCheck(): IO[HealthCheckOutput] = IO.pure {
    HealthCheckOutput("ok")
  }

  def smithy4sConvert(content: String): IO[Smithy4sConvertOutput] = {
    IO.println(content)
      .as(Smithy4sConvertOutput("resulst"))
  }
  def smithyValidate(content: String): IO[Unit] = {
    IO.delay(Validate.validateContent(content)).flatMap {
      case Right(value) => IO.unit
      case Left(value)  => IO.raiseError(InvalidSmithyContent(value.toList))
    }
  }
}

object Routes {
  import org.http4s.server.middleware.CORS
  private val example: Resource[IO, HttpRoutes[IO]] =
    SimpleRestJsonBuilder
      .routes(SmithyCodeGenerationServiceImpl)
      .resource
      .map(CORS(_))

  private val docs: HttpRoutes[IO] =
    smithy4s.http4s.swagger.docs[IO](SmithyCodeGenerationService)

  val all: Resource[IO, HttpRoutes[IO]] =
    example.map(_ <+> docs <+> Frontend.routes)
}

object Main extends IOApp.Simple {
  val run = Routes.all.flatMap { routes =>
    val thePort = port"9000"
    val theHost = host"0.0.0.0"
    EmberServerBuilder
      .default[IO]
      .withPort(thePort)
      .withHost(theHost)
      .withHttpApp(routes.orNotFound)
      .build <*
      Resource.eval(IO.println(s"Server started on: $theHost:$thePort"))
  }.useForever

}
