package io.github.ntdesmond.serdobot
package domain
package schedule

import java.time.LocalDate

case class DaySchedule(
  date: LocalDate,
  header: String,
  timeSlots: Set[TimeSlot],
  classNames: Set[ClassName],
  lessons: Map[(TimeSlotId, ClassNameId), Lesson],
):
  private lazy val timeslotMap = timeSlots.map(timeSlot => timeSlot.id -> timeSlot).toMap

  def getClassSchedule(number: Int, letter: Option[String]): Option[ClassSchedule] =
    classNames
      .find { className =>
        className.number == number && letter.forall(_ == className.letter)
      }
      .map(getClassSchedule)

  def getClassSchedule(className: ClassName): ClassSchedule =
    ClassSchedule(
      className = className,
      lessons = lessons
        .toList
        .sortBy { case ((tid, _), _) => timeslotMap.get(tid).map(_.start) }
        .withFilter { case ((_, cid), _) => cid == className.id }
        .map { case ((tid, cid), lesson) => (timeslotMap(tid), lesson) },
    )

  def getAllClassSchedules: Set[ClassSchedule] =
    classNames.map(getClassSchedule)

object DaySchedule:
  def make(
    date: LocalDate,
    header: String,
    timeslots: Iterable[TimeSlot],
    classnames: Iterable[ClassName],
    lessons: Iterable[Lesson],
  ): DaySchedule =
    DaySchedule(
      date,
      header,
      timeslots.toSet,
      classnames.toSet, {
        for
          lesson <- lessons
          cid    <- lesson.classNames
          tid    <- lesson.timeSlots
        yield (tid, cid) -> lesson
      }.toMap,
    )
