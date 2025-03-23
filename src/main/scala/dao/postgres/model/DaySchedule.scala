package io.github.ntdesmond.serdobot
package dao.postgres.model

import domain.schedule.LessonId
import io.getquill.JsonbValue
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl.*
import java.time.LocalDate
import java.time.LocalTime

case class DaySchedule(date: LocalDate, header: String):
  def toDomain(
    timeslots: Iterable[domain.schedule.TimeSlot],
    domainLessons: Iterable[domain.schedule.Lesson],
  ): domain.schedule.DaySchedule =
    this
      .into[domain.schedule.DaySchedule]
      .withFieldConst(_.timeSlots, timeslots.toSet)
      .withFieldConst(_.lessons, domainLessons.toSet)
      .transform
