package smithy4s_codegen.components.pages

import com.raquo.airstream.web.AjaxStream
import com.raquo.airstream.web.AjaxStream.AjaxStreamError
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom.ext.Ajax.InputData
import smithy4s_codegen.BuildInfo.baseUri
import smithy4s_codegen.components.CodeEditor
import smithy4s_codegen.api.SmithyValidateError
import smithy4s_codegen.api.SmithyValidateInput
import smithy4s_codegen.api.Smithy4sConvertOutput
import smithy4s_codegen.api.Smithy4sConvertInput
import smithy4s_codegen.components.CodeEditor.ValidationResult
import smithy4s_codegen.components.CodeViewer

import scalajs.js.JSON.stringify
import scalajs.js.JSON.parse

def Main() = {
  val editor = new CodeEditor()
  val viewer = new CodeViewer()

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
        .map { r => CodeEditor.ValidationResult.Success(value) }
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

  val convertedToSmithy4s: EventStream[CodeEditor.Smithy4sConversionResult] =
    validate.compose {
      _.collect { case ValidationResult.Success(content) =>
        content
      }
        .flatMap { value =>
          AjaxStream
            .post(
              url = s"$baseUri/smithy4s/convert",
              data = stringify {
                val payload = new Smithy4sConvertInput
                payload.content = value
                payload
              },
              headers = Map("Content-Type" -> "application/json")
            )
            .map { r =>
              val result =
                parse(r.responseText).asInstanceOf[Smithy4sConvertOutput]
              val content = scalajs.js.Object
                .entries(result.generated)
                .map { case scalajs.js.Tuple2(key, content) =>
                  key -> content.asInstanceOf[String]
                }
                .toMap
              CodeEditor.Smithy4sConversionResult.Success(content)
            }
            .recover { case ex: Throwable =>
              Some(CodeEditor.Smithy4sConversionResult.UnknownFailure(ex))
            }
        }
    }

  val (validateResultIcon, validateResultErrors) =
    editor.validationResult(validate)

  div(
    cls := "container mx-auto w-full h-full py-2 flex flex-row",
    div(
      cls := "h-full basis-1/2 p-2 relative",
      editor.component,
      div(
        cls := "absolute top-2 right-3",
        validateResultIcon
      )
    ),
    div(
      cls := "h-full basis-1/2 p-2",
      validateResultErrors,
      viewer.component(convertedToSmithy4s)
    )
  )
}
