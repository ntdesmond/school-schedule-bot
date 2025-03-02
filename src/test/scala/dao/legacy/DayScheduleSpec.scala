package io.github.ntdesmond.serdobot
package dao.legacy

import zio.Scope
import zio.ZIO
import zio.json.DecoderOps
import zio.test.*

import scala.io.Codec
import scala.io.Source

object DayScheduleSpec extends SerdobotSpec:
  private val testSchedule = DaySchedule(
    None, // 09.01, but ignored for simpler parsing
    "Четверг 9 января",
    Map(
      "Звонки" -> List(
        "8:30 - 8:55",
        "9:00 - 9:40",
        "9:55 - 10:35",
        "10:50 - 11:30",
        "11:45 - 12:25",
        "12:40 - 13:20",
        "13:30 - 14:10",
        "14:20 - 15:00",
        "15:10 - 15:50",
        "16:00 - 16:40",
        "16:50 - 17:30",
        "17:40 - 18:20",
      ),
      "11а" -> List(
        "ин/п 56 хим/п",
        "ОБиЗР 01",
        "геом/п/33 алг/б/43",
        "общ/п 32",
        "геом/п 33 хим/э 46",
        "мат/п 33 био/п ин/б 53",
        "общ/п 32 био/э",
        "био 45",
      ),
      "11б" -> List(
        "46ин/п 56 хим/п 46",
        "рус 21",
        "<<<",
        "общ/п 32 инф2",
        "<<<",
        "<<<",
        "<<<",
        "ОБиЗР 01",
      ),
      "10а" -> List(
        "Проф 54",
        "общ/п 32 физ/п 54 инф/п",
        "общ/п 32 физ/п хим/п 46 инф/п",
        "рус 34",
        "ин/п/б 52/53/44/56",
        "ин/п 52 физ/п 54",
        "геом п/33 ВС/б2/35",
        "ист 24",
        "ФК",
      ),
      "10б" -> List(
        "Проф 53",
        "<<<",
        "<<<",
        "ОБиЗР 01",
        "<<<",
        "<<<",
        "<<<",
        "инф2 54",
        "физ 54",
      ),
      "9а" -> List(
        "Проф 01",
        "гео 22",
        "рус 55",
        "геом 35",
        "труд 25/ТР",
        "лит 55",
        "ин 52/25",
        "хим 46",
      ),
      "9б" -> List(
        "",
        "ист 24",
        "ОБиЗР 01",
        "рус 21",
        "геом 31",
        "хим 46",
        "гео 22",
        "ин 41/25",
      ),
      "8а" -> List(
        "Проф 43",
        "хим 46",
        "муз 24",
        "лит 41",
        "геом 43",
        "алг 43",
        "ин 41/56",
        "ФК",
        "",
        "-",
      ),
      "8б" -> List(
        "Проф 41",
        "лит 55",
        "ФК",
        "геом 33",
        "ОБиЗР 01",
        "инф1 42",
        "хим 46",
        "физ 54",
      ),
      "7а" -> List(
        "Проф 32",
        "геом 33",
        "рус 41",
        "ин 52/56",
        "ФК",
        "гео 22",
        "лит 41",
      ),
      "7б" -> List(
        "Проф 21",
        "геом 43",
        "ин 53/56",
        "гео 22",
        "био 45",
        "рус 21",
        "ФК",
      ),
      "7в" -> List(
        "Проф 55",
        "ин 53/56",
        "гео 22",
        "физ 54",
        "лит 55",
        "муз 24",
        "геом 31",
      ),
      "6а" -> List(
        "Проф 24",
        "рус 35",
        "рус 34",
        "ин 53/45",
        "мат 21",
        "ист 31",
      ),
      "6б" -> List(
        "Проф 46",
        "рус 45",
        "мат 31",
        "мат 31",
        "муз 24",
        "ФК",
      ),
      "6в" -> List(
        "Проф 34",
        "рус 34",
        "мат 35",
        "муз 24",
        "мат 35",
        "био 45",
      ),
      "5а" -> List(
        "Кл.час 22",
        "мат 44",
        "ист 45",
        "ФК",
        "рус 22",
        "ин 25/41",
      ),
      "5б" -> List(
        "Кл.час 44",
        "рус 41",
        "труд 25/ТР",
        "труд 25/ТР",
        "ист 41",
        "мат 44",
        "МК 44",
      ),
    ),
  )

  def spec: Spec[TestEnvironment & Scope, Any] = suite("DayScheduleSpec")(
    test("json decode and to domain") {
      for
        scheduleFile <- ZIO
          .attempt(Source.fromResource("09.01.json")(Codec("utf-8")).mkString)
        schedule       <- ZIO.fromEither(scheduleFile.fromJson[DaySchedule])
        domainSchedule <- schedule.toDomain
      yield assertTrue(
        schedule == testSchedule,
        domainSchedule.header == testSchedule.dayInfo,
        domainSchedule.timeSlots == testSchedule
          .columns("Звонки")
          .map(domain.schedule.TimeSlot.fromString(_).toOption.get),
      )
    },
  )
