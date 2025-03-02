package io.github.ntdesmond.serdobot
package scalapy.modules.camelot

import me.shadaj.scalapy.py
import me.shadaj.scalapy.py.PyQuote

import scala.language.adhocExtensions

@py.native
class TableList extends py.Object:
  def toList: List[PdfTable] = py"list($this)".as[List[PdfTable]]
