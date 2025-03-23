package io.github.ntdesmond.serdobot
package domain
package schedule

import java.time.LocalDate

case class DaySchedule(
  date: LocalDate,
  header: String,
  timeSlots: Set[TimeSlot],
  lessons: Set[Lesson],
):
  def classNames: Set[ClassName] = lessons.flatMap(_.classNames)

  def getClassSchedule(number: Int, letter: Option[String]): Option[ClassSchedule] =
    def filter(className: ClassName) = className.number == number && letter
      .forall(_ == className.letter)

    classNames
      .find(filter)
      .map { className =>
        ClassSchedule(
          className = className,
          lessons = lessons.filter(_.classNames.exists(filter)).toList.sortBy(_.timeSlot.start),
        )
      }

  def getClassSchedule(className: ClassName): ClassSchedule =
    ClassSchedule(
      className = className,
      lessons = lessons.filter(_.classNames.contains(className)).toList.sortBy(_.timeSlot.start),
    )

  def getAllClassSchedules: Set[ClassSchedule] =
    classNames.map(getClassSchedule)
