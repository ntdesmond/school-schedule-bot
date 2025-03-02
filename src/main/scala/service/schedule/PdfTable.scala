package io.github.ntdesmond.serdobot
package service.schedule

import scala.language.adhocExtensions
import me.shadaj.scalapy.py

@py.native
class PdfTable extends py.Object:
  def cells: List[List[PdfCell]] = py.native
