package smithy4s_codegen.components

import com.raquo.laminar.api.L._

class CodeEditor() {
  private val initial = """|$version: "2"
                           |
                           |namespace input
                           |
                           |structure Person {
                           |  @required
                           |  name: String
                           |}""".stripMargin
  val codeContent = Var(initial)

  val component = {
    div(
      textArea(
        value := initial,
        onMountFocus,
        onInput.mapToValue --> codeContent
      )
    )
  }
}
