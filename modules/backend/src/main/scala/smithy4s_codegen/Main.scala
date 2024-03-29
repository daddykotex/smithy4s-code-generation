package smithy4s_codegen

import cats.effect._
import cats.effect.std.Env
import cats.effect.syntax.resource._
import cats.implicits._
import com.comcast.ip4s._
import org.http4s._
import org.http4s.ember.server._
import org.http4s.implicits._
import smithy4s.http4s.SimpleRestJsonBuilder
import smithy4s_codegen.api._
import smithy4s_codegen.generation._
import smithy4s_codegen.smithy._

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import scala.concurrent.duration._

class SmithyCodeGenerationServiceImpl(
    deps: List[String],
    generator: Smithy4s,
    validator: Validate
) extends SmithyCodeGenerationService[IO] {
  private val defaultDeps = List.empty[String] // TODO
  def healthCheck(): IO[HealthCheckOutput] = IO.pure {
    HealthCheckOutput("ok")
  }

  def getConfiguration(): IO[GetConfigurationOutput] =
    IO.pure(GetConfigurationOutput(deps.map(Dependency.apply)))

  def smithy4sConvert(
      content: String,
      deps: Option[List[Dependency]]
  ): IO[Smithy4sConvertOutput] = {
    generator
      .generate(deps.map(_.map(_.value)).getOrElse(defaultDeps), content)
      .leftMap(errors => InvalidSmithyContent(errors.map(_.getMessage)))
      .liftTo[IO]
      .map {
        _.map { case (path, r) =>
          Path(s"$path/${r.name}.scala") -> Content(r.content)
        }.toMap
      }
      .map(Smithy4sConvertOutput(_))
  }
  def smithyValidate(
      content: String,
      deps: Option[List[Dependency]]
  ): IO[Unit] = {
    validator
      .validateContent(deps.map(_.map(_.value)).getOrElse(defaultDeps), content)
      .flatMap {
        case Right(value) => IO.unit
        case Left(value)  => IO.raiseError(InvalidSmithyContent(value.toList))
      }
  }
}

object Routes {
  def route(config: Config): Resource[IO, HttpRoutes[IO]] =
    Resource
      .eval(ModelLoader(config.smithyClasspathConfig))
      .map(ml => (new Validate(ml), new Smithy4s(ml)))
      .flatMap { case (validator, generator) =>
        SimpleRestJsonBuilder
          .routes(
            new SmithyCodeGenerationServiceImpl(
              config.smithyClasspathConfig.entries.keySet.toList,
              generator,
              validator
            )
          )
          .resource
      }

  private val docs: HttpRoutes[IO] =
    smithy4s.http4s.swagger.docs[IO](SmithyCodeGenerationService)

  def fullRoutes(routes: HttpRoutes[IO]): HttpRoutes[IO] =
    routes <+> docs <+> Frontend.routes
}

object Main extends IOApp.Simple {
  val server = for {
    config <- Config.makeConfig.toResource
    routes <- Routes.route(config).map(Routes.fullRoutes)
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
