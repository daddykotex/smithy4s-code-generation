package smithy4s_codegen.components.pages

import com.raquo.airstream.ownership.ManualOwner
import com.raquo.laminar.api.L._
import smithy4s_codegen.api._
import smithy4s_codegen.components.CodeEditor
import smithy4s_codegen.components.CodeEditor.ValidationResult
import smithy4s_codegen.components.CodeViewer
import smithy4s_codegen.components.PermalinkCodec

object Home {
  def apply(
      api: SmithyCodeGenerationService[EventStream],
      config: EventStream[Either[Throwable, GetConfigurationOutput]]
  ) = {
    val editor = new CodeEditor(config.map(_.map(_.availableDependencies)))
    val viewer = new CodeViewer()

    locally {
      implicit val owner = new ManualOwner
      editor.codeContent.signal.foreach(PermalinkCodec.write)
    }

    val validate: EventStream[CodeEditor.ValidationResult] =
      editor.codeContent.signal
        .composeChanges(_.debounce(2000))
        .combineWith(editor.checkedDependencies.signal)
        .flatMap { case (code, deps) =>
          api
            .smithyValidate(code, Some(deps.toList))
            .map(_ =>
              CodeEditor.ValidationResult.Success(code, Some(deps.toList))
            )
            .recover {
              case InvalidSmithyContent(errors) =>
                Some(CodeEditor.ValidationResult.Failed(errors))
              case ex =>
                Some(CodeEditor.ValidationResult.UnknownFailure(ex))
            }
        }

    val convertedToSmithy4s: EventStream[CodeEditor.Smithy4sConversionResult] =
      validate.compose {
        _.collect { case ValidationResult.Success(code, deps) =>
          (code, deps)
        }
          .flatMap { case (code, deps) =>
            api
              .smithy4sConvert(code, deps)
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
