package smithy4s_codegen.components

import com.raquo.laminar.api.L._
import smithy4s_codegen.api.Content
import smithy4s_codegen.api.Path

object CodeEditor {
  sealed trait ValidationResult
  object ValidationResult {
    case object Loading extends ValidationResult
    case class Success(content: String) extends ValidationResult
    case class Failed(errors: List[String]) extends ValidationResult
    case class UnknownFailure(ex: Throwable) extends ValidationResult
  }

  sealed trait Smithy4sConversionResult
  object Smithy4sConversionResult {
    case object Loading extends Smithy4sConversionResult
    case class Success(content: Map[Path, Content])
        extends Smithy4sConversionResult
    case class UnknownFailure(ex: Throwable) extends Smithy4sConversionResult
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
        cls := "block p-2.5 w-full h-full text-sm text-gray-900 bg-gray-50 rounded-lg border border-gray-300 focus:ring-blue-500 focus:border-blue-500",
        value := initial,
        onMountFocus,
        onInput.mapToValue --> codeContent
      )
    )

  def validationResult(
      validationResult: EventStream[CodeEditor.ValidationResult]
  ) = {
    def displayIfHasErrors = styleAttr <-- validationResult.map(res =>
      if (res.isInstanceOf[CodeEditor.ValidationResult.Failed]) "display: block"
      else "display: none"
    )
    val errors = div(
      displayIfHasErrors,
      child.text <-- validationResult.collect {
        case CodeEditor.ValidationResult.Failed(errors) => errors.mkString("\n")
      }
    )
    val icon = ResultIcon(validationResult.map {
      case CodeEditor.ValidationResult.Loading    => ResultIcon.State.Loading
      case CodeEditor.ValidationResult.Success(_) => ResultIcon.State.Success
      case CodeEditor.ValidationResult.Failed(_)  => ResultIcon.State.Failed
      case CodeEditor.ValidationResult.UnknownFailure(_) =>
        ResultIcon.State.Failed
    })
    (icon, errors)
  }
}
