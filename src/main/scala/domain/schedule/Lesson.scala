package io.github.ntdesmond.serdobot
package domain
package schedule

import zio.prelude.Subtype
import zio.UIO
import zio.Random

import java.util.UUID

case class Lesson(id: LessonId, name: String, timeSlot: TimeSlot, classNames: Set[ClassName]):
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

object Lesson:
  def apply(name: String, timeSlot: TimeSlot, classNames: Set[ClassName]): UIO[Option[Lesson]] =
    val cleanName = """\s+""".r.replaceAllIn(name, " ").strip()
    Random
      .nextUUID
      .map { uuid =>
        Lesson(
          id = LessonId(uuid),
          name = cleanName,
          timeSlot = timeSlot,
          classNames = classNames,
        )
      }
      .when(cleanName.nonEmpty && cleanName != "-")

object LessonId extends Subtype[UUID]
type LessonId = LessonId.Type
