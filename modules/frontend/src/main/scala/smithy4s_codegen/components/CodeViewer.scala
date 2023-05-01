package smithy4s_codegen.components

import com.raquo.laminar.api.L.{*, given}
import smithy4s_codegen.components.CodeEditor.Smithy4sConversionResult

class CodeViewer() {
  def component(content: EventStream[CodeEditor.Smithy4sConversionResult]) = {
    val success: EventStream[List[(String, String)]] = content.collect {
      case Smithy4sConversionResult.Success(content) => content.toList
    }
    val fileAndContent: Signal[List[HtmlElement]] =
      success.split(_._1)(render)

    div(
      icon(content),
      div(
        children <-- fileAndContent
      )
    )
  }

  private def render(
      path: String,
      initial: (String, String),
      signal: Signal[(String, String)]
  ): HtmlElement =
    div(
      p("path: " + path),
      div("content: ", code(pre(child.text <-- signal.map(_._2))))
    )

  private def icon(
      validationResult: EventStream[Smithy4sConversionResult]
  ) = {
    def toImageSrc(c: Smithy4sConversionResult): String = {
      c match {
        case Smithy4sConversionResult.Loading =>
          "loading-spinner-svgrepo-com.svg"
        case Smithy4sConversionResult.Success(_) =>
          "checkmark-svgrepo-com.svg"
        case Smithy4sConversionResult.UnknownFailure(_) =>
          "cross-svgrepo-com.svg"
      }
    }
    def toAlt(v: Smithy4sConversionResult): String = {
      v match {
        case Smithy4sConversionResult.Loading           => "Loading"
        case Smithy4sConversionResult.Success(_)        => "Success"
        case Smithy4sConversionResult.UnknownFailure(_) => "Failed"
      }
    }
    div(
      img(
        cls := "w-8 h-8",
        alt <-- validationResult.map(toAlt),
        src <-- validationResult.map(toImageSrc).map("images/" + _)
      )
    )
  }
}
