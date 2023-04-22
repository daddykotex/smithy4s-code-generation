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
      cls := "h-full",
      textArea(
        cls := "block p-2.5 w-full h-full text-sm text-gray-900 bg-gray-50 rounded-lg border border-gray-300 focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 dark:focus:border-blue-500",
        value := initial,
        onMountFocus,
        onInput.mapToValue --> codeContent
      )
    )
  }
}
