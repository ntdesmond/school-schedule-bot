package io.github.ntdesmond.serdobot
package service.schedule

import tofu.logging.zlogs.TofuZLogger
import zio.Scope
import zio.ZIO
import zio.ZLayer
import zio.test.*

object ScheduleSpec extends SerdobotSpec:
  override val bootstrap: ZLayer[Any, Any, TestEnvironment] =
    zio.Runtime.removeDefaultLoggers >>>
      TofuZLogger.addToRuntime >>>
      testEnvironment

  def spec: Spec[TestEnvironment & Scope, Any] = suite("ScheduleSpec")(
    List("09.02.pdf", "21.02.pdf").map { filename =>
      test(s"Parse $filename") {
        for
          date <- zio.Clock.instant.map(java.util.Date.from)
          path <- zio
            .ZIO
            .fromNullable(PdfScheduleParser.getClass.getResource(s"/$filename"))
            .map(_.getPath.stripPrefix("/"))
          schedule <- PdfScheduleParser.parseFile(date, path)
          _        <- zio.Console.printLine(schedule.header)
          _ <- ZIO
            .foreachDiscard(schedule.classSchedules)(zio.Console.printLine(_))
        yield assertCompletes
      }
    },
  )
