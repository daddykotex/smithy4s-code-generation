package smithy4s_codegen

import cats.effect.IO
import cats.effect.IOApp
import com.raquo.laminar.api.L._
import org.scalajs.dom
import smithy4s_codegen.api.SmithyCodeGenerationService
import smithy4s_codegen.components.pages.Home
import scala.concurrent.duration.Duration

object Main extends IOApp.Simple {
  // fully disable the checker
  override def runtimeConfig =
    super.runtimeConfig.copy(cpuStarvationCheckInitialDelay = Duration.Inf)

  private def setup(api: SmithyCodeGenerationService[EventStream]) = IO.delay {
    lazy val appContainer = dom.document.querySelector("#app") // must be lazy
    val appElement =
      Home(api, api.getConfiguration().recoverToTry.map(_.toEither))
    render(appContainer, appElement)
  }
  val run = ApiBuilder.build.flatMap(setup(_).toResource).useForever
}
