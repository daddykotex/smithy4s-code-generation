package smithy4s_codegen.components

import com.raquo.laminar.api.L._
import smithy4s_codegen.api.Content
import smithy4s_codegen.api.Path
import smithy4s_codegen.bindings.lzstring
import smithy4s_codegen.api.Dependencies
import smithy4s_codegen.api.Dependency

object CodeEditor {
  sealed trait ValidationResult
  object ValidationResult {
    case object Loading extends ValidationResult
    case class Success(editorContent: EditorContent) extends ValidationResult
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
class CodeEditor(dependencies: EventStream[Either[Throwable, Dependencies]]) {
  private val initial = """|$version: "2"
                           |
                           |namespace input
                           |
                           |structure Person {
                           |  @required
                           |  name: String
                           |}""".stripMargin
  val editorContent = Var(
    PermalinkCodec
      .readOnce()
      .getOrElse(EditorContent(initial, Set.empty))
  )

  val updatePermalinkCode = {
    val v = onInput.mapToValue.map(value => editorContent.now().copy(value))
    v --> editorContent
  }

  val updateValueFromPermalinkCode =
    value <-- editorContent.signal.map(_.code)

  def updatePermalinkDeps(dep: Dependency) = {
    val mod = { (isChecked: Boolean) =>
      editorContent.update { content =>
        val newSet =
          if (isChecked) content.deps + dep
          else content.deps - dep
        content.copy(deps = newSet)
      }
    }
    onChange.mapToChecked --> mod
  }

  def updateCheckFromPermalinkDeps(dep: Dependency) = {
    checked <-- editorContent.signal.map { content =>
      content.deps.find(_ == dep).isDefined
    }
  }

  val dependenciesCheckboxes = {
    def displayIfHasErrors = styleAttr <-- dependencies.map(res =>
      if (res.isLeft) "display: block"
      else "display: none"
    )
    val errors = div(
      displayIfHasErrors,
      child.text <-- dependencies.collect { case Left(ex) =>
        "Unable to get available dependencies"
      }
    )
    val depsList = div(
      children <-- dependencies.collect { case Right(deps) =>
        List(
          fieldSet(
            legend("Choose your dependencies"),
            deps.value.map { dep =>
              div(
                input(
                  cls := "m-2",
                  `type` := "checkbox",
                  nameAttr := dep.value,
                  idAttr := dep.value,
                  updatePermalinkDeps(dep),
                  updateCheckFromPermalinkDeps(dep)
                ),
                label(forId := dep.value, dep.value)
              )
            }
          )
        )
      }
    )
    div(errors, depsList)
  }

  val component =
    div(
      cls := "h-full",
      textArea(
        cls := "block p-2.5 w-full h-5/6 text-sm text-gray-900 bg-gray-50 rounded-lg border border-gray-300 focus:ring-blue-500 focus:border-blue-500 font-mono",
        onMountFocus,
        controlled(
          updateValueFromPermalinkCode,
          updatePermalinkCode
        )
      ),
      div(
        cls := "block p-2.5 w-full h-1/6",
        dependenciesCheckboxes
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

final case class EditorContent(code: String, deps: Set[Dependency])

/** Writes code to the URL hash and provides a stream of its decoded values.
  *
  * Encoding/decoding of code is handled internally.
  */
object PermalinkCodec {
  val hashTag = "#"
  val hashTagLength = hashTag.length()
  val hashPart = ";"

  def readOnce(): Option[EditorContent] =
    decode(org.scalajs.dom.window.location.hash)

  val read: EventStream[EditorContent] = windowEvents(_.onHashChange)
    .mapTo(org.scalajs.dom.window.location.hash)
    .map(decode(_))
    .collectSome

  def write(value: EditorContent): Unit =
    org.scalajs.dom.window.location.hash = encode(value)

  private class HashPartValue(partName: String) {
    private val partKey = s"$partName="
    def encode(value: String): String = s"$partKey$value"
    def unapply(value: String): Option[String] = {
      if (value.startsWith(partKey)) Some(value.drop(partKey.length()))
      else None
    }
  }
  private val codePart = new HashPartValue("code")
  private val depsPart = new HashPartValue("dependencies")

  private def encode(value: EditorContent): String = {
    val code =
      codePart.encode(lzstring.compressToEncodedURIComponent(value.code))
    val deps = depsPart.encode(value.deps.map(_.value).mkString(","))
    val hash = List(code, deps).mkString(";")
    s"#$hash"
  }

  private def decode(hash: String): Option[EditorContent] = {
    if (hash.startsWith(hashTag)) {
      val hashParts = hash
        .drop(hashTagLength)
        .split(hashPart)

      val maybeCode = hashParts.collectFirst { case codePart(value) =>
        Option(lzstring.decompressFromEncodedURIComponent(value))
      }.flatten
      val deps =
        hashParts
          .collectFirst { case depsPart(value) =>
            value
              .split(",")
              .filter(_.nonEmpty)
              .toSet
              .map(Dependency(_))
          }
          .getOrElse(Set.empty)
      maybeCode.map(code => EditorContent(code, deps))
    } else None
  }
}
