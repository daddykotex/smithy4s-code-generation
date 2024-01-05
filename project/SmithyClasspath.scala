package smithy4s_codegen

import sbt.librarymanagement.ModuleID
import sbt.librarymanagement.Disabled
import java.io.File
import sbt.io.IO

final case class SmithyClasspathEntry(
    module: sbt.ModuleID,
    file: File
)
final case class SmithyClasspathDockerEntry(
    module: sbt.ModuleID,
    file: File,
    relativePath: String
)
object SmithyClasspath {
  def jsonConfig(
      target: File,
      all: Seq[(ModuleID, String)]
  ): Unit = {
    val entries = all.map { case (module, location) =>
      encodeModule(module) -> ujson.Str(location)
    }.toMap
    val content = ujson.Obj(
      "entries" -> ujson.Obj.from(entries)
    )
    IO.write(target, content.render())
  }

  private def encodeModule(m: ModuleID): String = {
    m.crossVersion match {
      case Disabled => s"${m.organization}:${m.name}:${m.revision}"
    }
  }
}
