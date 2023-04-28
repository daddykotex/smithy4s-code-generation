package smithy4s_codegen.smithy

import cats.data.NonEmptyList
import software.amazon.smithy.model.Model
import scala.jdk.CollectionConverters._

def validateContent(content: String): Either[NonEmptyList[String], Unit] = {
  val res = Model.assembler().addUnparsedModel("ui.smithy", content).assemble()
  if (res.isBroken()) {
    val errorList = res.getValidationEvents().asScala.toList.map(_.getMessage())
    Left(NonEmptyList.of(errorList.head, errorList.tail: _*))
  } else {
    Right(())
  }
}
