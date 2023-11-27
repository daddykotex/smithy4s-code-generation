package smithy4s_codegen.components

import com.raquo.laminar.api.L._

object ResultIcon {
  sealed trait State
  object State {
    case object Loading extends State
    case object Success extends State
    case object Failed extends State
  }

  def apply(result: EventStream[ResultIcon.State]) =
    img(
      cls <-- result.map(imgClass),
      alt <-- result.map(toAlt),
      src <-- result.map(toImageSrc).map("images/" + _)
    )
  private def imgClass(v: ResultIcon.State) = v match {
    case ResultIcon.State.Failed => "w-8 h-8 scale-[0.70] text-red"
    case _                       => "w-8 h-8"
  }

  private def toImageSrc(v: ResultIcon.State): String =
    v match {
      case ResultIcon.State.Loading => "loading-spinner-svgrepo-com.svg"
      case ResultIcon.State.Success => "checkmark-svgrepo-com.svg"
      case ResultIcon.State.Failed  => "cross-svgrepo-com.svg"
    }

  private def toAlt(v: ResultIcon.State): String =
    v match {
      case ResultIcon.State.Loading => "Loading"
      case ResultIcon.State.Success => "Success"
      case ResultIcon.State.Failed  => "Failed"
    }
}
