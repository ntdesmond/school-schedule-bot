package io.github.ntdesmond.serdobot
package service.schedule

import scala.language.adhocExtensions
import me.shadaj.scalapy.py
import me.shadaj.scalapy.py.PyQuote

@py.native
class TableList extends py.Object:
  def toList: List[PdfTable] = py"list($this)".as[List[PdfTable]]
