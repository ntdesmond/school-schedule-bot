package io.github.ntdesmond.serdobot
package dao.legacy

import domain.schedule.ClassSchedule

import zio.json.JsonDecoder
import zio.json.ast.Json
import zio.json.ast.JsonCursor

import java.util.Date

case class DaySchedule(
  date: Option[Date],
  dayInfo: String,
  columns: Map[String, List[String]],
):
  def toDomain: zio.IO[domain.DomainError, domain.schedule.DaySchedule] =
    val timeColumnName = "Звонки"

    for
      now <- zio.Clock.instant.map(Date.from)
      timeColumn <- zio
        .ZIO
        .getOrFailWith(domain.ParseError("No time column found"))(columns
          .get(timeColumnName))
      timeSlots <- zio
        .ZIO
        .foreach(timeColumn) { slot =>
          zio.ZIO.fromEither(domain.schedule.TimeSlot.fromString(slot))
        }
      classSchedules <- zio
        .ZIO
        .foreach(columns.removed(timeColumnName).toList)(columnToDomain)
    yield domain
      .schedule
      .DaySchedule(date.getOrElse(now), dayInfo, classSchedules, timeSlots)

  private def columnToDomain(className: String, lessons: List[String]): zio.IO[
    domain.DomainError,
    ClassSchedule,
  ] =
    for
      className <- zio
        .ZIO
        .fromEither(
          domain.ClassName.fromString(className),
        )
      lessons <- zio
        .ZIO
        .foreach(lessons) { lessonName =>
          zio
            .Random
            .nextUUID
            .map { uuid =>
              domain.schedule.Lesson(domain.schedule.LessonId(uuid), lessonName)
            }
        }
    yield ClassSchedule(className, lessons)

object DaySchedule:
  private val dayInfoCursor =
    JsonCursor.field("dayInfo") >>> JsonCursor.isString

  given JsonDecoder[DaySchedule] = JsonDecoder[Json].mapOrFail { json =>
    for
      dayInfo     <- json.get(dayInfoCursor)
      updatedJson <- json.delete(dayInfoCursor)
      columns     <- updatedJson.as[Map[String, List[String]]]
    yield DaySchedule(None, dayInfo.value, columns)
  }
