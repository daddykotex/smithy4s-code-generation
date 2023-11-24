package smithy4s_codegen.components.pages

import com.raquo.airstream.ownership.ManualOwner
import com.raquo.laminar.api.L._
import smithy4s_codegen.api.SmithyCodeGenerationService
import smithy4s_codegen.components.CodeEditor
import smithy4s_codegen.components.CodeEditor.ValidationResult
import smithy4s_codegen.components.CodeViewer

import smithy4s_codegen.api.InvalidSmithyContent
import smithy4s_codegen.components.PermalinkCodec

object Home {
  def apply(api: SmithyCodeGenerationService[EventStream]) = {
    val editor = new CodeEditor()
    val viewer = new CodeViewer()

    locally {
      implicit val owner = new ManualOwner
      editor.codeContent.signal.foreach(PermalinkCodec.write)
    }

    val validate: EventStream[CodeEditor.ValidationResult] =
      editor.codeContent.signal
        .composeChanges(_.debounce(2000))
        .flatMap { value =>
          api
            .smithyValidate(value)
            .map(_ => CodeEditor.ValidationResult.Success(value))
            .recover {
              case InvalidSmithyContent(errors) =>
                Some(CodeEditor.ValidationResult.Failed(errors))
              case ex =>
                Some(CodeEditor.ValidationResult.UnknownFailure(ex))
            }
        }

    val convertedToSmithy4s: EventStream[CodeEditor.Smithy4sConversionResult] =
      validate.compose {
        _.collect { case ValidationResult.Success(content) =>
          content
        }
          .flatMap { value =>
            api
              .smithy4sConvert(value)
              .map(r =>
                CodeEditor.Smithy4sConversionResult.Success(r.generated)
              )
              .recover { ex =>
                Some(CodeEditor.Smithy4sConversionResult.UnknownFailure(ex))
              }
          }
      }

    val (validateResultIcon, validateResultErrors) =
      editor.validationResult(validate)

    div(
      cls := "container mx-auto h-full py-2 flex",
      div(
        cls := "h-full p-2 relative basis-1/2",
        editor.component,
        div(
          cls := "absolute top-2 right-3",
          validateResultIcon
        )
      ),
      div(
        cls := "h-full p-2 basis-1/2 overflow-x-scroll",
        validateResultErrors,
        viewer.component(convertedToSmithy4s)
      )
    )
  }
}
