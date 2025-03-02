package io.github.ntdesmond.serdobot
package scalapy.modules

import scalapy.modules.io.StringIO

import me.shadaj.scalapy.py

import scala.language.dynamics

@py.native
object ContextlibModule extends py.StaticModule("contextlib"):
  def redirect_stderr(io: StringIO): py.Dynamic = py.native
