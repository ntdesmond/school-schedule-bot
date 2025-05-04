package io.github.ntdesmond.serdobot
package dao
package postgres

import domain.schedule.*
import io.getquill.*
import io.scalaland.chimney.dsl.*
import java.time.LocalDate
import javax.sql.DataSource
import zio.Task
import zio.ZEnvironment
import zio.ZIO
import zio.ZLayer

class DayScheduleDAOImpl(dataSource: DataSource) extends DayScheduleDAO:
  import PostgresContext.*

  private def scheduleDataQuery(date: LocalDate) = transaction {
    for
      subheaders <- run(Schema.subheader.filter(_.date == lift(date)))
        .map(_.map(_.transformInto[domain.schedule.Subheader]))
      lessons <- run {
        for
          timeslot        <- Schema.timeslot.filter(_.date == lift(date))
          classTimeLesson <- Schema.classTimeLesson.leftJoin(timeslot.id == _.timeslotId)
          lesson <- Schema.lesson.leftJoin(l => classTimeLesson.map(_.lessonId).contains(l.id))
          classnames <- Schema
            .className
            .leftJoin(cn => classTimeLesson.map(_.classNameId).contains(cn.id))
        yield (timeslot, lesson, classnames)
      }
      (timeSlots, classNames, lessonMap) = lessons.foldLeft((
        Set.empty[domain.schedule.TimeSlot],
        Set.empty[domain.ClassName],
        Map.empty[LessonId, domain.schedule.Lesson],
      )) {
        case ((timeslots, classnames, lessons), (timeslot, maybeLesson, maybeClassname)) =>
          (
            timeslots + timeslot.transformInto[domain.schedule.TimeSlot],
            classnames ++ maybeClassname.map(_.transformInto[domain.ClassName]),
            lessons ++ maybeLesson
              .zip(maybeClassname)
              .map { case (lesson, classname) =>
                lesson.id -> lessons
                  .getOrElse(lesson.id, lesson.toDomain(List.empty, List.empty))
                  .appendClassNameId(classname.id)
                  .appendTimeSlot(timeslot.id)
              },
          )
      }
    yield (subheaders, timeSlots, classNames, lessonMap)
  }

  override def get(date: LocalDate): Task[Option[DaySchedule]] = {
    for
      maybeSchedule <- run(Schema.daySchedule.filter(_.date == lift(date)).take(1))
        .map(_.headOption)
      maybeScheduleData <- ZIO.foreach(maybeSchedule.map(_.date))(scheduleDataQuery)
    yield maybeSchedule
      .zip(maybeScheduleData)
      .map { case (schedule, (subheaders, timeslots, classnames, lessons)) =>
        schedule.toDomain(timeslots, classnames, lessons.values, subheaders)
      }
  }.provideEnvironment(ZEnvironment(dataSource))

  override def save(schedule: DaySchedule): Task[Unit] =
    transaction {
      val dbSchedule = schedule.transformInto[model.DaySchedule]

      for
        _ <- ZIO.foreachDiscard(schedule.timeSlots) { timeslot =>
          val dbTimeslot = timeslot.transformInto[model.TimeSlot]
          run {
            Schema
              .timeslot
              .insertValue(lift(dbTimeslot))
              .onConflictUpdate(_.id)(
                (t, e) => t.date -> e.date,
                (t, e) => t.start -> e.start,
                (t, e) => t.end -> e.end,
              )
          }
        }
        // Insert/Update ClassNames
        _ <- ZIO.foreachDiscard(schedule.classNames) { className =>
          val dbClassName = className.transformInto[model.ClassName]
          run {
            Schema
              .className
              .insertValue(lift(dbClassName))
              .onConflictUpdate(_.id)(
                (t, e) => t.date -> e.date,
                (t, e) => t.number -> e.number,
                (t, e) => t.letter -> e.letter,
                (t, e) => t.additionalData -> e.additionalData,
              )
          }
        }
        // Insert/Update Lessons
        _ <- ZIO.foreachDiscard(schedule.lessons.values.toSet) { lesson =>
          val dbLesson = lesson.transformInto[model.Lesson]
          run {
            Schema
              .lesson
              .insertValue(lift(dbLesson))
              .onConflictUpdate(_.id)((t, e) => t.name -> e.name)
          }
        }
        // Insert ClassTimeLesson relationships (ignore conflicts as they represent existing valid links)
        _ <- ZIO.foreachDiscard(schedule.lessons) { case ((tid, cid), lesson) =>
          val record = model
            .ClassTimeLesson(classNameId = cid, timeslotId = tid, lessonId = lesson.id)
          run(Schema.classTimeLesson.insertValue(lift(record)).onConflictIgnore)
        }
        // Insert/Update Subheaders
        _ <- ZIO.foreachDiscard(schedule.subheaders) { subheader =>
          val dbSubheader = subheader.transformInto[model.Subheader]
          run {
            Schema
              .subheader
              .insertValue(lift(dbSubheader))
              .onConflictUpdate(_.id)(
                (t, e) => t.date -> e.date,
                (t, e) => t.timeslotId -> e.timeslotId,
                (t, e) => t.content -> e.content,
              )
          }
        }
        // Upsert DaySchedule header info (should happen after related entities are handled or within the same transaction scope)
        _ <- run(Schema
          .daySchedule
          .insertValue(lift(dbSchedule))
          .onConflictUpdate(_.date)((t, e) => t.header -> e.header))
      yield ()
    }.provideEnvironment(ZEnvironment(dataSource))

object DayScheduleDAOImpl:
  // noinspection TypeAnnotation
  def layer = ZLayer.derive[DayScheduleDAOImpl]
