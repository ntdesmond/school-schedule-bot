package io.github.ntdesmond.serdobot
package dao.postgres.model

import java.time.LocalDate

case class DaySchedule(date: LocalDate, header: String):
  def toDomain(
    timeslots: Iterable[domain.schedule.TimeSlot],
    classnames: Iterable[domain.ClassName],
    domainLessons: Iterable[domain.schedule.Lesson],
  ): domain.schedule.DaySchedule =
    domain
      .schedule
      .DaySchedule
      .make(
        date,
        header,
        timeslots,
        classnames,
        domainLessons,
      )
