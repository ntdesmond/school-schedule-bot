package io.github.ntdesmond.serdobot
package domain

case class ClassName(number: Int, letter: String, misc: Option[String]):
  def asFormattedString: String =
    s"$number$letter" + misc.fold("")(value => s" $value")

object ClassName:
  def fromString(s: String): Either[ParseError, ClassName] =
    val pattern = """(?i)(\d{1,2})\s*(\p{L}\b)\s*(.*)""".r
    for
      (number, letter, misc) <-
        s match
          case pattern(number, letter, misc) =>
            Right((number, letter, Option(misc).filter(_.nonEmpty)))
          case _ => Left(ParseError(s"Invalid class name: $s"))
      validatedNumber <- number
        .toIntOption
        .filter(number => number >= 1 && number <= 11)
        .toRight(ParseError(s"Invalid class number: $number"))
    yield ClassName(validatedNumber, letter, misc)
