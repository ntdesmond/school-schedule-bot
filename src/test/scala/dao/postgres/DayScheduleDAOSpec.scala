package io.github.ntdesmond.serdobot
package dao.postgres

import com.zaxxer.hikari.HikariDataSource
import dao.legacy.DaySchedule
import domain.schedule.LessonName
import domain.schedule.TimeSlot
import domain.schedule.TimeSlotId
import io.getquill.*
import java.nio.file.Paths
import java.util.UUID
import javax.sql.DataSource
import scala.io.Codec
import scala.io.Source
import tofu.logging.zlogs.TofuZLogger
import zio.Chunk
import zio.Scope
import zio.ZEnvironment
import zio.ZIO
import zio.ZLayer
import zio.json.DecoderOps
import zio.test.*

object DayScheduleDAOSpec extends SerdobotItSpec:
  def spec: Spec[Environment & Scope, Any] = suite("DayScheduleDAOSpec")(
    for
      dao            <- ZIO.service[dao.DayScheduleDAO]
      scheduleFile   <- ZIO.attempt(Source.fromResource("09.01.json")(Codec("utf-8")).mkString)
      schedule       <- ZIO.fromEither(scheduleFile.fromJson[DaySchedule])
      domainSchedule <- schedule.toDomain
      _              <- dao.save(domainSchedule)
    yield Chunk(
      test("save and get") {
        for
          result <- dao.get(domainSchedule.date)
        yield assertTrue(result.contains(domainSchedule))
      },
      test("update") {
        val newHeader = "New header"
        val newTimeslots = domainSchedule
          .timeSlots
          .map { t =>
            t.copy(start = t.start.plusMinutes(10))
          }
        val newLessons = domainSchedule
          .lessons
          .map {
            case (key, l) =>
              (key, l.copy(name = LessonName.wrap(LessonName.unwrap(l.name) + " - new")))
          }
        val newSchedule = domainSchedule.copy(
          header = newHeader,
          timeSlots = newTimeslots,
          lessons = newLessons,
        )

        for
          _      <- dao.save(newSchedule)
          result <- dao.get(domainSchedule.date)
        yield assertTrue(result.contains(newSchedule), newSchedule != domainSchedule)
      },
    ),
  ) @@ TestAspect.sequential
