package io.github.ntdesmond.serdobot
package dao
package postgres

import domain.schedule.DaySchedule
import domain.schedule.Lesson
import domain.schedule.LessonId
import io.getquill.*
import io.scalaland.chimney.dsl.*
import java.util.Date
import javax.sql.DataSource
import zio.Task
import zio.ZEnvironment
import zio.ZIO

class DayScheduleDAOImpl(dataSource: DataSource) extends DayScheduleDAO:
  import PostgresContext.*

  override def get(date: Date): Task[Option[DaySchedule]] = {
    for
      maybeSchedule <- run(Schema.daySchedule.filter(_.date == lift(date)).take(1))
        .map(_.headOption)
      maybeDomainLessons <- ZIO.foreach(maybeSchedule) { schedule =>
        run {
          for
            timeslot    <- Schema.timeslot.filter(timeslot => timeslot.date == lift(schedule.date))
            lesson      <- Schema.lesson.leftJoin(_.timeSlotId == timeslot.id)
            classLesson <- Schema.classLesson.leftJoin(cl => lesson.map(_.id).contains(cl.lessonId))
            classnames <- Schema
              .className
              .leftJoin(cn => classLesson.map(_.classNameId).contains(cn.id))
          yield (timeslot, lesson, classnames)
        }.map(_.foldLeft((
          Set.empty[domain.schedule.TimeSlot],
          Map.empty[LessonId, domain.schedule.Lesson],
        )) {
          case ((timeslots, lessons), (timeslot, maybeLesson, maybeClassname)) =>
            (
              timeslots + timeslot.transformInto[domain.schedule.TimeSlot],
              lessons ++ maybeLesson
                .zip(maybeClassname)
                .map { case (lesson, classname) =>
                  lesson.id -> lessons
                    .getOrElse(
                      lesson.id,
                      lesson.toDomain(timeslot.transformInto[domain.schedule.TimeSlot], List.empty),
                    )
                    .appendClassName(classname.transformInto[domain.ClassName])
                },
            )
        })
      }
    yield maybeSchedule
      .zip(maybeDomainLessons)
      .map { case (schedule, (timeslots, lessons)) => schedule.toDomain(timeslots, lessons.values) }
  }.provideEnvironment(ZEnvironment(dataSource))

  override def save(date: Date, schedule: DaySchedule): Task[Unit] =
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
        _ <- ZIO.foreachDiscard(schedule.lessons) { lesson =>
          val dbLesson = model.Lesson.fromDomain(lesson)
          run {
            Schema
              .lesson
              .insertValue(lift(dbLesson))
              .onConflictUpdate(_.id)(
                (t, e) => t.name -> e.name,
                (t, e) => t.timeSlotId -> e.timeSlotId,
              )
          } *>
            ZIO.foreachDiscard(lesson.classNames) { className =>
              val dbClassLesson = model.ClassLesson(
                lessonId = dbLesson.id,
                classNameId = className.id,
              )
              run(Schema.classLesson.insertValue(lift(dbClassLesson)).onConflictIgnore)
            }
        }
        _ <- run(Schema
          .daySchedule
          .insertValue(lift(dbSchedule))
          .onConflictUpdate(_.date)((t, e) => t.header -> e.header))
      yield ()
    }.provideEnvironment(ZEnvironment(dataSource))
