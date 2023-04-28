package smithy4s_codegen.components.pages

import com.raquo.airstream.web.AjaxStream
import com.raquo.airstream.web.AjaxStream.AjaxStreamError
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom.ext.Ajax.InputData
import smithy4s_codegen.BuildInfo.baseUri
import smithy4s_codegen.components.CodeEditor
import smithy4s_codegen.api.SmithyValidateInput
import smithy4s_codegen.api.SmithyValidateError

import scalajs.js.JSON.stringify
import scalajs.js.JSON.parse

def Main() = {
  val editor = new CodeEditor()
  val validate = editor.codeContent.signal
    .composeChanges(_.debounce(2000))
    .flatMap { value =>
      AjaxStream
        .post(
          url = s"$baseUri/smithy/validate",
          data = stringify {
            val payload = new SmithyValidateInput
            payload.content = value
            payload
          },
          headers = Map("Content-Type" -> "application/json")
        )
        .map { r => CodeEditor.ValidationResult.Success }
        .recover {
          case ex: AjaxStreamError =>
            if (ex.xhr.status != 400)
              Some(CodeEditor.ValidationResult.UnknownFailure(ex))
            else {
              val result =
                parse(ex.xhr.responseText).asInstanceOf[SmithyValidateError]
              Some(CodeEditor.ValidationResult.Failed(result.errors.toList))
            }
          case ex: Throwable =>
            Some(CodeEditor.ValidationResult.UnknownFailure(ex))
        }
    }

  div(
    cls := "container mx-auto columns-2 w-full h-full py-2",
    div(
      cls := "left-pane w-full h-full",
      editor.component
    ),
    div(
      cls := "right-pane w-full h-full",
      editor.validationResult(validate),
      span(child.text <-- editor.codeContent)
    )
  )
}
