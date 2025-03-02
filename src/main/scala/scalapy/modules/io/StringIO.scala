package io.github.ntdesmond.serdobot
package scalapy.modules.io

import me.shadaj.scalapy.py
import scala.language.adhocExtensions

@py.native
class StringIO extends py.Object:
  def getvalue(): String = py.native
