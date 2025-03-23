package io.github.ntdesmond.serdobot
package dao
package postgres

import domain.schedule.DaySchedule
import domain.schedule.Lesson
import domain.schedule.LessonId
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

  override def get(date: LocalDate): Task[Option[DaySchedule]] = {
    for
      maybeSchedule <- run(Schema.daySchedule.filter(_.date == lift(date)).take(1))
        .map(_.headOption)
      maybeDomainLessons <- ZIO.foreach(maybeSchedule) { schedule =>
        run {
          for
            timeslot <- Schema.timeslot.filter(timeslot => timeslot.date == lift(schedule.date))
            classTimeLesson <- Schema.classTimeLesson.leftJoin(timeslot.id == _.timeslotId)
            lesson <- Schema.lesson.leftJoin(l => classTimeLesson.map(_.lessonId).contains(l.id))
            classnames <- Schema
              .className
              .leftJoin(cn => classTimeLesson.map(_.classNameId).contains(cn.id))
          yield (timeslot, lesson, classnames)
        }.map(_.foldLeft((
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
        })
      }
    yield maybeSchedule
      .zip(maybeDomainLessons)
      .map { case (schedule, (timeslots, classnames, lessons)) =>
        schedule.toDomain(timeslots, classnames, lessons.values)
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
        _ <- ZIO.foreachDiscard(schedule.lessons.values.toSet) { lesson =>
          val dbLesson = lesson.transformInto[model.Lesson]
          run {
            Schema
              .lesson
              .insertValue(lift(dbLesson))
              .onConflictUpdate(_.id)((t, e) => t.name -> e.name)
          }
        }
        _ <- ZIO.foreachDiscard(schedule.lessons) { case ((tid, cid), lesson) =>
          val record = model.ClassTimeLesson(
            classNameId = cid,
            timeslotId = tid,
            lessonId = lesson.id,
          )
          run(Schema.classTimeLesson.insertValue(lift(record)).onConflictIgnore)
        }
        _ <- run(Schema
          .daySchedule
          .insertValue(lift(dbSchedule))
          .onConflictUpdate(_.date)((t, e) => t.header -> e.header))
      yield ()
    }.provideEnvironment(ZEnvironment(dataSource))

object DayScheduleDAOImpl:
  // noinspection TypeAnnotation
  def layer = ZLayer.derive[DayScheduleDAOImpl]
