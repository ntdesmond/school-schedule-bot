package io.github.ntdesmond.serdobot
package service.schedule

import tofu.logging.zlogs.TofuZLogger
import zio.Scope
import zio.ZIO
import zio.ZLayer
import zio.test.*
import java.nio.file.Paths

object ScheduleSpec extends SerdobotSpec:
  override val bootstrap: ZLayer[Any, Any, TestEnvironment] =
    zio.Runtime.removeDefaultLoggers >>>
      TofuZLogger.addToRuntime >>>
      testEnvironment

  def spec: Spec[TestEnvironment & Scope, Any] = suite("ScheduleSpec")(
    List(
      "09.02.pdf",
      "21.02.pdf",
      "14.03.pdf",
      "15.03.pdf",
    ).map { filename =>
      test(s"Parse $filename") {
        for
          date <- zio.Clock.instant.map(java.util.Date.from)
          path <- ZIO
            .fromNullable(PdfScheduleParser.getClass.getResource(s"/$filename"))
            .mapBoth(_ => "File not found", url => Paths.get(url.toURI).toString)
          schedule <- PdfScheduleParser.parseFile(date, path)
        yield assertCompletes
      }
    },
  )
