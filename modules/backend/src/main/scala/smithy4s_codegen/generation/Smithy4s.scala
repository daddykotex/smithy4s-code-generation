package smithy4s_codegen.generation

import cats.syntax.all._
import cats.data.NonEmptyList
import cats.effect.syntax.all._
import software.amazon.smithy.model.Model
import scala.jdk.CollectionConverters._
import smithy4s.codegen._
import java.nio.file.Path
import cats.effect.Resource
import cats.effect.IO
import java.nio.file.Files

final case class Smithy4sWorkspace(
    input: os.Path,
    output: os.Path,
    specs: List[os.Path]
)

object Smithy4s {
  def generate(content: String): IO[Vector[(Path, String)]] = {
    val prepare = for {
      workspace <- newWorkspace
      _ <- IO.delay(os.makeDir(workspace.input)).toResource
      smithyFilePath = workspace.input / "frontend.smithy"
      _ <- IO
        .delay(os.write(smithyFilePath, content))
        .toResource
    } yield workspace.copy(specs = workspace.specs :+ smithyFilePath)
    prepare.use { workspace =>
      val args = CodegenArgs(
        workspace.specs,
        workspace.output,
        resourceOutput = workspace.output,
        skip = Set(FileType.Resource, FileType.Openapi),
        discoverModels = true,
        allowedNS = None,
        excludedNS = None,
        dependencies = List.empty,
        repositories = List.empty,
        transformers = List.empty,
        localJars = List.empty
      )
      IO.delay(Codegen.processSpecs(args))
        .flatMap(withContents(workspace.output))
    }
  }

  private def newWorkspace: Resource[IO, Smithy4sWorkspace] =
    fs2.io.file
      .Files[IO]
      .tempDirectory
      .map { path =>
        val osPath = os.Path(path.toNioPath)
        Smithy4sWorkspace(
          input = osPath / "input",
          output = osPath / "output",
          specs = List.empty
        )
      }

  private def withContents(
      output: os.Path
  )(paths: Set[os.Path]): IO[Vector[(Path, String)]] =
    paths.toVector.traverse { p =>
      IO.delay(p.relativeTo(output).toNIO -> os.read(p))
    }

}
