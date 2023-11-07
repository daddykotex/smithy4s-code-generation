package smithy4s_codegen

import cats.effect.IO
import cats.effect.IOApp
import cats.effect.kernel.Resource
import cats.effect.syntax.resource._
import com.raquo.laminar.api.L._
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.dom.FetchClientBuilder
import org.scalajs.dom
import smithy4s.http4s.SimpleRestJsonBuilder
import smithy4s_codegen.api.SmithyCodeGenerationService
import smithy4s_codegen.components.pages.Home

object Main extends IOApp.Simple {
  private def setup(api: SmithyCodeGenerationService[EventStream]) = IO.delay {
    lazy val appContainer = dom.document.querySelector("#app") // must be lazy
    val appElement = Home(api)
    render(appContainer, appElement)
  }
  val run = ApiBuilder.build.flatMap(setup(_).toResource).useForever
}
