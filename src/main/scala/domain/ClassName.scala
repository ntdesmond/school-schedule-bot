package io.github.ntdesmond.serdobot
package domain

case class ClassName(number: Int, letter: String)

object ClassName:
  def fromString(s: String): Either[ParseError, ClassName] =
    val pattern = """(?i)(\d{1,2})\s*(\p{L})""".r
    for
      parsed <-
        s match
          case pattern(number, letter) => Right((number, letter))
          case _ => Left(ParseError(s"Invalid class name: $s"))
      (number, letter) = parsed
      validatedNumber <- number
        .toIntOption
        .filter(number => number >= 1 && number <= 11)
        .toRight(ParseError(s"Invalid class number: $number"))
    yield ClassName(validatedNumber, letter)
