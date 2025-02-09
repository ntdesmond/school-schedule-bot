package io.github.ntdesmond.serdobot
package domain
package schedule

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import scala.util.Try

case class TimeSlot(start: LocalTime, end: LocalTime)

object TimeSlot:
  def fromString(s: String): Either[ParseError, TimeSlot] =
    val pattern = """(\d{1,2}:\d{2})\s*-\s*(\d{1,2}:\d{2})""".r
    for
      parsed <-
        s match
          case pattern(start, end) => Right((start, end))
          case _                   => Left(ParseError(s"Invalid time slot: $s"))
      (start, end) = parsed
      validatedStart <- parseTime(start)
      validatedEnd <- parseTime(end)
    yield TimeSlot(validatedStart, validatedEnd)

  private def parseTime(s: String): Either[ParseError, LocalTime] =
    Try(LocalTime.parse(s, DateTimeFormatter.ofPattern("H:mm")))
      .toEither
      .left
      .map(_ => ParseError(s"Invalid time: $s"))
