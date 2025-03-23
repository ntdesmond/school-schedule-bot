package io.github.ntdesmond.serdobot
package service.schedule

import java.nio.file.Paths
import java.time.LocalDate
import tofu.logging.zlogs.TofuZLogger
import zio.Scope
import zio.ZIO
import zio.ZLayer
import zio.test.*

object ScheduleSpec extends SerdobotSpec:
  def spec: Spec[TestEnvironment & Scope, Any] = suite("ScheduleSpec")(
    List(
      "09.02.pdf",
      "21.02.pdf",
      "14.03.pdf",
      "15.03.pdf",
    ).map { filename =>
      test(s"Parse $filename") {
        for
          date <- zio.Clock.localDateTime.map(_.toLocalDate)
          path <- ZIO
            .fromNullable(PdfScheduleParser.getClass.getResource(s"/$filename"))
            .mapBoth(_ => "File not found", url => Paths.get(url.toURI).toString)
          schedule <- PdfScheduleParser.parseFile(date, path)
        yield assertCompletes
      }
    },
  )
