package io.github.ntdesmond.serdobot
package domain
package schedule

import java.util.Date

case class DaySchedule(
  date: Date,
  header: String,
  timeSlots: Set[TimeSlot],
  lessons: Set[Lesson],
):
  def classNames: Set[ClassName] = lessons.flatMap(_.classNames)

  def getClassSchedule(className: ClassName): ClassSchedule =
    ClassSchedule(
      className = className,
      lessons = lessons.filter(_.classNames.contains(className)).toList.sortBy(_.timeSlot.start),
    )

  def getAllClassSchedules: Set[ClassSchedule] =
    classNames.map(getClassSchedule)
