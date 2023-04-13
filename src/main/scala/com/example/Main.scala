package com.example

import smithy4s.hello._
import cats.effect._
import cats.implicits._
import org.http4s.implicits._
import org.http4s.ember.server._
import org.http4s._
import com.comcast.ip4s._
import smithy4s.http4s.SimpleRestJsonBuilder

object HelloWorldImpl extends HelloWorldService[IO] {
  def healthCheck(): IO[HealthCheckOutput] = IO.pure {
    HealthCheckOutput("ok")
  }
}

object Routes {
  private val example: Resource[IO, HttpRoutes[IO]] =
    SimpleRestJsonBuilder.routes(HelloWorldImpl).resource

  private val docs: HttpRoutes[IO] =
    smithy4s.http4s.swagger.docs[IO](HelloWorldService)

  val all: Resource[IO, HttpRoutes[IO]] = example.map(_ <+> docs)
}

object Main extends IOApp.Simple {
  val run = Routes.all.flatMap { routes =>
    val thePort = port"9000"
    val theHost = host"localhost"
    EmberServerBuilder
      .default[IO]
      .withPort(thePort)
      .withHost(theHost)
      .withHttpApp(routes.orNotFound)
      .build <*
      Resource.eval(IO.println(s"Server started on: $theHost:$thePort"))
  }.useForever

}
