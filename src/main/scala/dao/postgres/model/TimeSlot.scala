package io.github.ntdesmond.serdobot
package dao.postgres.model

import domain.schedule.TimeSlotId
import java.time.LocalDate
import java.time.LocalTime

case class TimeSlot(id: TimeSlotId, date: LocalDate, start: LocalTime, end: LocalTime)
