package io.github.ntdesmond.serdobot
package dao

import domain.schedule.DaySchedule

import zio.Task

import java.util.Date

trait DayScheduleDAO:
  def get(date: Date): Task[Option[DaySchedule]]
  def save(date: Date, schedule: DaySchedule): Task[Unit]
