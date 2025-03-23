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
  timeSlots: Set[TimeSlotId],
  classNames: Set[ClassNameId],
):
  def appendClassNameId(classNameId: ClassNameId): Lesson =
    copy(classNames = classNames + classNameId)

  def appendTimeSlot(otherTimeSlotId: TimeSlotId): Lesson =
    copy(timeSlots = timeSlots + otherTimeSlotId)

object LessonId extends Subtype[UUID] with PostgresSubtype[UUID] with MakeRandomUUID
type LessonId = LessonId.Type

object LessonName extends Subtype[String] with PostgresSubtype[String]:
  def fromString(value: String): Option[LessonName] =
    Option("""\s+""".r.replaceAllIn(value, " ").strip())
      .filter(name => name.nonEmpty && name != "-")
      .map(apply)
type LessonName = LessonName.Type
