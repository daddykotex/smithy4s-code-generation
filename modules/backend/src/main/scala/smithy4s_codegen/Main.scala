package smithy4s_codegen

import cats.effect._
import cats.effect.std.Env
import cats.effect.syntax.resource._
import cats.implicits._
import com.comcast.ip4s._
import org.http4s._
import org.http4s.ember.server._
import org.http4s.implicits._
import smithy4s._
import smithy4s.http4s.SimpleRestJsonBuilder
import smithy4s_codegen.api._
import smithy4s_codegen.generation._
import smithy4s_codegen.smithy._

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import scala.concurrent.duration._

class SmithyCodeGenerationServiceImpl(generator: Smithy4s, validator: Validate)
    extends SmithyCodeGenerationService[IO] {
  def healthCheck(): IO[HealthCheckOutput] = IO.pure {
    HealthCheckOutput("ok")
  }

  def smithy4sConvert(content: String): IO[Smithy4sConvertOutput] = {
    generator
      .generate(content)
      .leftMap(errors => InvalidSmithyContent(errors.map(_.getMessage)))
      .liftTo[IO]
      .map {
        _.map { case (path, r) =>
          Path(path.toString()) -> Content(r.content)
        }.toMap
      }
      .map(Smithy4sConvertOutput(_))
  }
  def smithyValidate(content: String): IO[Unit] = {
    validator.validateContent(content).flatMap {
      case Right(value) => IO.unit
      case Left(value)  => IO.raiseError(InvalidSmithyContent(value.toList))
    }
  }
}

object Routes {
  import org.http4s.server.middleware.CORS
  def exampleRoute(localJars: List[File]): Resource[IO, HttpRoutes[IO]] =
    Resource
      .eval(ModelLoader(localJars))
      .map(ml => (new Validate(ml), new Smithy4s(ml)))
      .flatMap { case (validator, generator) =>
        SimpleRestJsonBuilder
          .routes(new SmithyCodeGenerationServiceImpl(generator, validator))
          .resource
          .map(CORS(_))
      }

  private val docs: HttpRoutes[IO] =
    smithy4s.http4s.swagger.docs[IO](SmithyCodeGenerationService)

  def fullRoutes(routes: HttpRoutes[IO]): HttpRoutes[IO] =
    routes <+> docs <+> Frontend.routes
}

object Main extends IOApp.Simple {

  val envSmithyClasspath: IO[List[File]] = Env[IO]
    .get("APP_SMITHY_CLASSPATH")
    .map(
      _.map(_.trim().split(":").toList)
        .getOrElse(List.empty)
    )
    .flatMap(files =>
      files.traverse { fileLocation =>
        val path = Paths.get(fileLocation)
        IO.delay(Files.exists(path))
          .ifM(
            path.toFile.pure[IO],
            IO.raiseError(
              new IllegalArgumentException(
                "APP_SMITHY_CLASSPATH contains bad values."
              )
            )
          )

      }
    )

  val server = for {
    smithyClasspath <- envSmithyClasspath.toResource
    routes <- Routes.exampleRoute(smithyClasspath).map(Routes.fullRoutes)
    thePort = port"9000"
    theHost = host"0.0.0.0"
    res <-
      EmberServerBuilder
        .default[IO]
        .withPort(thePort)
        .withHost(theHost)
        .withHttpApp(routes.orNotFound)
        .withShutdownTimeout(5.seconds)
        .build
    _ <- Resource.eval(IO.println(s"Server started on: $theHost:$thePort"))
  } yield res
  override val run = server.useForever

}
