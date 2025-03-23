package io.github.ntdesmond.serdobot
package domain
package schedule

import io.github.ntdesmond.serdobot.dao.postgres.PostgresSubtype
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.UUID
import scala.util.Try
import zio.prelude.Subtype

case class TimeSlot(id: TimeSlotId, date: Date, start: LocalTime, end: LocalTime)

object TimeSlot:
  def fromString(id: TimeSlotId, date: Date, s: String): Either[ParseError, TimeSlot] =
    val pattern = """(\d{1,2}:\d{2})\D+(\d{1,2}:\d{2})""".r.unanchored
    for
      parsed <-
        s match
          case pattern(start, end) => Right((start, end))
          case _                   => Left(ParseError(s"Invalid time slot: $s"))
      (start, end) = parsed
      validatedStart <- parseTime(start)
      validatedEnd   <- parseTime(end)
    yield TimeSlot(id, date, validatedStart, validatedEnd)

  private def parseTime(s: String): Either[ParseError, LocalTime] =
    Try(LocalTime.parse(s, DateTimeFormatter.ofPattern("H:mm")))
      .toEither
      .left
      .map(_ => ParseError(s"Invalid time: $s"))

object TimeSlotId extends Subtype[UUID] with PostgresSubtype[UUID] with MakeRandomUUID
type TimeSlotId = TimeSlotId.Type
