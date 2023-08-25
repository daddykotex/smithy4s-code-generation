package smithy4s_codegen.generation

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.Resource
import cats.effect.syntax.all._
import cats.syntax.all._
import smithy4s.codegen._
import software.amazon.smithy.model.Model

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import scala.jdk.CollectionConverters._

final case class Smithy4sWorkspace(
    input: os.Path,
    output: os.Path,
    specs: List[os.Path]
)

final class Smithy4s(localJars: List[File]) {
  def generate(
      content: String
  ): IO[Vector[(Path, String)]] = {
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
        localJars = localJars.map(os.Path(_))
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
