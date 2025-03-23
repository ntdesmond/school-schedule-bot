package io.github.ntdesmond.serdobot
package domain
package schedule

import dao.postgres.PostgresSubtype
import io.github.ntdesmond
import io.github.ntdesmond.serdobot
import java.util.Date
import java.util.UUID
import zio.prelude
import zio.prelude.Subtype

case class Lesson(
  id: LessonId,
  name: LessonName,
  timeSlot: TimeSlot,
  classNames: Set[ClassName],
):
  def appendClassName(className: ClassName): Lesson =
    copy(classNames = classNames + className)

  def extendTimeSlot(otherTimeSlot: TimeSlot): Either[DomainError, Lesson] =
    val newSlot =
      if (
        otherTimeSlot.start.isBefore(timeSlot.start) &&
        !timeSlot.start.isBefore(otherTimeSlot.end)
      )
        Right(timeSlot.copy(start = otherTimeSlot.start))
      else if (
        !timeSlot.end.isAfter(otherTimeSlot.start) &&
        otherTimeSlot.end.isAfter(timeSlot.end)
      )
        Right(timeSlot.copy(end = otherTimeSlot.end))
      else
        Left(Business("Overlapping timeslots"))

    newSlot.map(ts => copy(timeSlot = ts))

object LessonId extends Subtype[UUID] with PostgresSubtype[UUID] with MakeRandomUUID
type LessonId = LessonId.Type

object LessonName extends Subtype[String] with PostgresSubtype[String]:
  def fromString(value: String): Option[LessonName] =
    Option("""\s+""".r.replaceAllIn(value, " ").strip())
      .filter(name => name.nonEmpty && name != "-")
      .map(apply)
type LessonName = LessonName.Type
