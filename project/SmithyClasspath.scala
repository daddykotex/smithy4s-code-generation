package smithy4s_codegen

import sbt.librarymanagement.ModuleID
import sbt.librarymanagement.Disabled
import java.io.File
import sbt.io.IO

final case class SmithyClasspathEntry(
    module: sbt.ModuleID,
    file: File,
    relativePath: String
)
object SmithyClasspath {
  def toFile(
      target: File,
      all: Seq[SmithyClasspathEntry],
      dockerPath: String
  ): Unit = {
    val entries = all.map { sce =>
      encodeModule(sce.module) -> ujson.Str(s"$dockerPath/${sce.relativePath}")
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
