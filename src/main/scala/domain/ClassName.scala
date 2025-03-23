package io.github.ntdesmond.serdobot
package domain

import dao.postgres.PostgresSubtype
import java.util.Date
import java.util.UUID
import zio.prelude.Subtype

case class ClassName(
  id: ClassNameId,
  date: Date,
  number: Int,
  letter: String,
  additionalData: Option[String],
):
  def asFormattedString: String =
    s"$number$letter" + additionalData.fold("")(value => s" $value")

object ClassName:
  def fromString(id: ClassNameId, date: Date, s: String): Either[ParseError, ClassName] =
    val pattern = """(?i)(\d{1,2})\s*(\p{L}\b)\s*(.*)""".r
    for
      (number, letter, additionalData) <-
        s match
          case pattern(number, letter, additionalData) =>
            Right((number, letter, Option(additionalData).filter(_.nonEmpty)))
          case _ => Left(ParseError(s"Invalid class name: $s"))
      validatedNumber <- number
        .toIntOption
        .filter(number => number >= 1 && number <= 11)
        .toRight(ParseError(s"Invalid class number: $number"))
    yield ClassName(id, date, validatedNumber, letter, additionalData)

object ClassNameId extends Subtype[UUID] with PostgresSubtype[UUID] with MakeRandomUUID
type ClassNameId = ClassNameId.Type
