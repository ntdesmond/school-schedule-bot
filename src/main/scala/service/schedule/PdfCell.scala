package io.github.ntdesmond.serdobot
package service.schedule

import me.shadaj.scalapy.py

@py.native
class PdfCell extends py.Object:
  def x1: Double = py.native
  def y1: Double = py.native
  def x2: Double = py.native
  def y2: Double = py.native
  def lb: (Double, Double) = py.native
  def lt: (Double, Double) = py.native
  def rb: (Double, Double) = py.native
  def rt: (Double, Double) = py.native
  def left: Boolean = py.native
  def right: Boolean = py.native
  def top: Boolean = py.native
  def bottom: Boolean = py.native
  def text: String = py.native
