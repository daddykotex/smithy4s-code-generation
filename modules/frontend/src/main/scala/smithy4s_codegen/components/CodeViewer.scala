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

    val icon = ResultIcon(content.map {
      case Smithy4sConversionResult.Loading    => ResultIcon.State.Loading
      case Smithy4sConversionResult.Success(_) => ResultIcon.State.Success
      case Smithy4sConversionResult.UnknownFailure(_) => ResultIcon.State.Failed
    })

    div(
      icon,
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
      div(
        "content: ",
        code(
          cls := "block p-2.5 text-sm text-gray-900 bg-gray-50 rounded-lg border border-gray-300",
          pre(child.text <-- signal.map(_._2))
        )
      )
    )
}
