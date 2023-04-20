package smithy4s_codegen

import com.raquo.laminar.api.L._

import org.scalajs.dom

object Main {
  def main(args: Array[String]): Unit = {
    lazy val appContainer = dom.document.querySelector("#app") // must be lazy
    val appElement = div(h1("Hello world"))
    renderOnDomContentLoaded(appContainer, appElement)
  }
}
