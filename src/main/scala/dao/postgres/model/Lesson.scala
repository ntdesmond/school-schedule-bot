package io.github.ntdesmond.serdobot
package dao.postgres.model

import domain.schedule.LessonId
import domain.schedule.LessonName
import domain.schedule.TimeSlotId
import io.getquill.JsonbValue
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl.*
import java.util.Date

case class Lesson(id: LessonId, name: LessonName):
  def toDomain(
    timeSlots: List[domain.schedule.TimeSlotId],
    classNames: List[domain.ClassNameId],
  ): domain.schedule.Lesson =
    this
      .into[domain.schedule.Lesson]
      .withFieldConst(_.timeSlots, timeSlots.toSet)
      .withFieldConst(_.classNames, classNames.toSet)
      .transform
