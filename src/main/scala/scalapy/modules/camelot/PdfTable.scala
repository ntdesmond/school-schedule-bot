package io.github.ntdesmond.serdobot
package scalapy.modules.camelot

import me.shadaj.scalapy.py

import scala.language.adhocExtensions

@py.native
class PdfTable extends py.Object:
  def cells: List[List[PdfCell]] = py.native
