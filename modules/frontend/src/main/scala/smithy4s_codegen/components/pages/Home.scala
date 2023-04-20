package smithy4s_codegen.components.pages

import smithy4s_codegen.components.CodeEditor
import com.raquo.laminar.api.L._

def Main() = {
  val editor = CodeEditor()
  div(
    div(
      cls := "left-pane",
      editor.component
    ),
    div(
      cls := "right-pane",
      span(child.text <-- editor.codeContent)
    )
  )
}
