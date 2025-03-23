package io.github.ntdesmond.serdobot
package dao.postgres.model

import domain.schedule.LessonId
import domain.schedule.LessonName
import domain.schedule.TimeSlotId
import io.getquill.JsonbValue
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl.*
import java.util.Date

case class Lesson(id: LessonId, name: String, timeSlotId: TimeSlotId):
  def toDomain(
    timeSlot: domain.schedule.TimeSlot,
    classNames: List[domain.ClassName],
  ): domain.schedule.Lesson =
    this
      .into[domain.schedule.Lesson]
      .withFieldComputed(_.name, l => LessonName(l.name))
      .withFieldConst(_.timeSlot, timeSlot)
      .withFieldConst(_.classNames, classNames.toSet)
      .transform

object Lesson:
  def fromDomain(lesson: domain.schedule.Lesson): Lesson =
    lesson
      .into[Lesson]
      .withFieldComputed(_.name, l => LessonName.unwrap(l.name))
      .withFieldConst(_.timeSlotId, lesson.timeSlot.id)
      .transform
