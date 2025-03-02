package io.github.ntdesmond.serdobot
package scalapy.modules.io

import me.shadaj.scalapy.py
import scala.language.dynamics

@py.native
object IOModule extends py.StaticModule("io"):
  def StringIO(): StringIO = py.native
