package io.github.ntdesmond.serdobot
package scalapy

import scalapy.modules.ContextlibModule
import scalapy.modules.io.IOModule

import me.shadaj.scalapy.py
import zio.Task
import zio.ZIO

object ScalaPyExtensions:
  def attemptWithStderr[T](code: => T): Task[(T, String)] =
    ZIO.attempt {
      val buffer   = IOModule.StringIO()
      val redirect = ContextlibModule.redirect_stderr(buffer)
      val result   = py.`with`(redirect)(_ => code)
      (result, buffer.getvalue())
    }
