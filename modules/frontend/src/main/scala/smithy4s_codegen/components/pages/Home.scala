package smithy4s_codegen.components.pages

import com.raquo.airstream.web.AjaxStream
import com.raquo.airstream.web.AjaxStream.AjaxStreamError
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom.ext.Ajax.InputData
import smithy4s_codegen.BuildInfo.baseUri
import smithy4s_codegen.components.CodeEditor

import scalajs.js.JSON.stringify

class ValidatePayload extends scalajs.js.Object {
  var content: String = _
}

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
      p(
        child.text <-- editor.codeContent.signal.flatMap { value =>
          AjaxStream
            .post(
              url = s"$baseUri/smithy/validate",
              data = stringify {
                val payload = new ValidatePayload
                payload.content = value
                payload
              },
              headers = Map("Content-Type" -> "application/json")
            )
            .map("Response: " + _.responseText)
            .recover { case err: AjaxStreamError => Some(err.getMessage) }
        }
      ),
      span(child.text <-- editor.codeContent)
    )
  )
}
