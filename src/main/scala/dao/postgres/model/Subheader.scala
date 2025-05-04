package io.github.ntdesmond.serdobot
package dao
package postgres
package model

import domain.schedule.SubheaderId
import domain.schedule.TimeSlotId
import java.time.LocalDate

case class Subheader(
  id: SubheaderId,
  date: LocalDate,
  timeslotId: Option[TimeSlotId],
  content: String,
)
