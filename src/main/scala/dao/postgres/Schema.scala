package io.github.ntdesmond.serdobot
package dao.postgres

import io.getquill.EntityQuery
import io.getquill.querySchema

object Schema:
  import model.*
  inline def daySchedule: EntityQuery[DaySchedule]         = querySchema("day_schedule")
  inline def lesson: EntityQuery[Lesson]                   = querySchema("lesson")
  inline def timeslot: EntityQuery[TimeSlot]               = querySchema("timeslot")
  inline def className: EntityQuery[ClassName]             = querySchema("classname")
  inline def classTimeLesson: EntityQuery[ClassTimeLesson] = querySchema("class_time_lesson")
