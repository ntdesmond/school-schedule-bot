package io.github.ntdesmond.serdobot
package dao

import domain.schedule.DaySchedule
import java.time.LocalDate
import zio.Task

trait DayScheduleDAO:
  def get(date: LocalDate): Task[Option[DaySchedule]]
  def save(schedule: DaySchedule): Task[Unit]
