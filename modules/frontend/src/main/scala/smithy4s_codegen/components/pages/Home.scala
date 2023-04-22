package smithy4s_codegen.components.pages

import smithy4s_codegen.components.CodeEditor
import com.raquo.laminar.api.L._

def Main() = {
  val editor = CodeEditor()
  div(
    cls := "container mx-auto columns-2 w-full h-full py-2",
    div(
      cls := "left-pane w-full h-full",
      editor.component
    ),
    div(
      cls := "right-pane w-full h-full",
      span(child.text <-- editor.codeContent)
    )
  )
}
