package io.github.ntdesmond.serdobot
package dao.legacy

import domain.schedule.Lesson
import domain.schedule.LessonId
import domain.schedule.LessonName
import domain.schedule.TimeSlot
import domain.schedule.TimeSlotId
import io.github.ntdesmond.serdobot.domain.ClassNameId
import java.util.Date
import scala.collection.immutable.ListMap
import zio.IO
import zio.ZIO
import zio.json.JsonDecoder
import zio.json.ast.Json
import zio.json.ast.JsonCursor

case class DaySchedule(
  date: Option[Date],
  dayInfo: String,
  timeslots: List[String],
  columns: List[(String, List[String])],
):
  def toDomain: IO[domain.DomainError, domain.schedule.DaySchedule] =
    val timeColumnName = "Звонки"

    for
      today <- zio.Clock.instant.map(Date.from)
      date = this.date.getOrElse(today)
      timeSlots <- ZIO.foreach(timeslots) { slot =>
        TimeSlotId.makeRandom().map(TimeSlot.fromString(_, date, slot)).absolve
      }
      (lessons, _) <- ZIO
        .foldLeft(columns)((Map.empty[LessonId, Lesson], List.empty[Option[Lesson]])) {
          case ((acc, lastColumn), (classname, lessons)) =>
            columnToDomain(date, lastColumn, classname, lessons, timeSlots).map(column =>
              (acc ++ column.collect { case Some(lesson) => lesson.id -> lesson }, column),
            )
        }
    yield domain.schedule.DaySchedule(date, dayInfo, timeSlots.toSet, lessons.values.toSet)

  private def columnToDomain(
    date: Date,
    lastColumn: List[Option[Lesson]],
    className: String,
    lessons: List[String],
    timeslots: List[TimeSlot],
  ): IO[domain.DomainError, List[Option[Lesson]]] =
    for
      className <- ClassNameId
        .makeRandom()
        .map(domain.ClassName.fromString(_, date, className))
        .absolve
      lessons <- ZIO.foreach(lessons.zipAll(lastColumn, "", None).zip(timeslots)) {
        case (("<<<", lessonFromLeft), _) =>
          ZIO.succeed(lessonFromLeft.map(_.appendClassName(className)))
        case ((lessonName, _), timeslot) =>
          (LessonId.makeRandom() <*> ZIO.succeed(LessonName.fromString(lessonName))).map {
            case (id, name) => name.map(Lesson(id, _, timeslot, Set(className)))
          }
      }
    yield lessons

object DaySchedule:
  private val dayInfoCursor =
    JsonCursor.field("dayInfo") >>> JsonCursor.isString

  private val timesCursor =
    JsonCursor.field("Звонки") >>> JsonCursor.isArray

  given JsonDecoder[DaySchedule] = JsonDecoder[Json].mapOrFail { json =>
    for
      dayInfo     <- json.get(dayInfoCursor)
      timeslots   <- json.get(timesCursor).flatMap(_.as[List[String]])
      updatedJson <- json.delete(dayInfoCursor).flatMap(_.delete(timesCursor))
      columns     <- updatedJson.as[ListMap[String, List[String]]]
    yield DaySchedule(None, dayInfo.value, timeslots, columns.toList)
  }
