package io.github.ntdesmond.serdobot
package dao.postgres.model

import domain.schedule.TimeSlotId
import java.time.LocalTime
import java.util.Date

case class TimeSlot(id: TimeSlotId, date: Date, start: LocalTime, end: LocalTime)
