package smithy4s_codegen.components

import com.raquo.laminar.api.L.{*, given}
import smithy4s_codegen.components.CodeEditor.ValidationResult

object CodeEditor {
  enum ValidationResult {
    case Loading
    case Success(content: String)
    case Failed(errors: List[String])
    case UnknownFailure(ex: Throwable)
  }

  enum Smithy4sConversionResult {
    case Loading
    case Success(content: Map[String, String])
    case UnknownFailure(ex: Throwable)
  }
}
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

  val component =
    div(
      cls := "h-full",
      textArea(
        cls := "block p-2.5 w-full h-full text-sm text-gray-900 bg-gray-50 rounded-lg border border-gray-300 focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 dark:focus:border-blue-500",
        value := initial,
        onMountFocus,
        onInput.mapToValue --> codeContent
      )
    )

  def validationResult(
      validationResult: EventStream[CodeEditor.ValidationResult]
  ) = {
    def toImageSrc(v: ValidationResult): String = {
      v match {
        case ValidationResult.Loading    => "loading-spinner-svgrepo-com.svg"
        case ValidationResult.Success(_) => "checkmark-svgrepo-com.svg"
        case ValidationResult.Failed(_)  => "cross-svgrepo-com.svg"
        case ValidationResult.UnknownFailure(_) => "cross-svgrepo-com.svg"
      }
    }
    def toAlt(v: ValidationResult): String = {
      v match {
        case ValidationResult.Loading           => "Loading"
        case ValidationResult.Success(_)        => "Success"
        case ValidationResult.Failed(_)         => "Failed validation"
        case ValidationResult.UnknownFailure(_) => "Failed"
      }
    }
    def displayIfHasErrors = styleAttr <-- validationResult.map(res =>
      if (res.isInstanceOf[ValidationResult.Failed]) "display: block"
      else "display: none"
    )
    div(
      img(
        cls := "w-8 h-8",
        alt <-- validationResult.map(toAlt),
        src <-- validationResult.map(toImageSrc).map("images/" + _)
      ),
      div(
        displayIfHasErrors,
        child.text <-- validationResult.collect {
          case ValidationResult.Failed(errors) => errors.mkString("\n")
        }
      )
    )
  }
}
