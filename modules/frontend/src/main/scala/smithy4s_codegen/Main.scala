package smithy4s_codegen

import cats.effect.IO
import cats.effect.IOApp
import com.raquo.laminar.api.L._
import org.scalajs.dom
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
