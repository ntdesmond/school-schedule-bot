package io.github.ntdesmond.serdobot

import tofu.logging.zlogs.TofuZLogger
import zio.ZLayer
import zio.test.TestEnvironment
import zio.test.ZIOSpecDefault
import zio.test.testEnvironment

abstract class SerdobotSpec extends ZIOSpecDefault:
  override val bootstrap: ZLayer[Any, Any, TestEnvironment] =
    zio.Runtime.removeDefaultLoggers >>>
      TofuZLogger.addToRuntime >>>
      testEnvironment
