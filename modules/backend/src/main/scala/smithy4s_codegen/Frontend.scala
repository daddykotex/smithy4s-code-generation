package smithy4s_codegen

import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.headers.Location
import org.http4s.implicits._

object Frontend {
  private def static(file: String, request: Request[IO]) =
    StaticFile
      .fromResource("/dist/" + file, Some(request))
      .getOrElseF(NotFound())

  val routes = HttpRoutes.of[IO] {
    case request @ GET -> Root =>
      Found(Location.apply(uri"/index.html"))
    case request @ GET -> Root / "index.html" =>
      static("index.html", request)
    case request @ GET -> Root / "assets" / path =>
      static(s"assets/$path", request)
    case request @ GET -> Root / "images" / path =>
      static(s"images/$path", request)
  }
}
