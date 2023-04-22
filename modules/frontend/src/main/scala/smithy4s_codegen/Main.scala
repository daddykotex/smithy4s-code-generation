package smithy4s_codegen

import com.raquo.laminar.api.L._
import org.scalajs.dom
import smithy4s_codegen.components.pages.Main

@main def run(): Unit = {
  lazy val appContainer = dom.document.querySelector("#app") // must be lazy
  val appElement = Main()
  renderOnDomContentLoaded(appContainer, appElement)
}
