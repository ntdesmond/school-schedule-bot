package io.github.ntdesmond.serdobot
package domain
package schedule

import java.util.Date

case class DaySchedule(
  date: Date,
  header: String,
  classSchedules: List[ClassSchedule],
  timeSlots: List[TimeSlot],
)

object DaySchedule:
  def fromJson(json: String): Either[ParseError, DaySchedule] = ???
