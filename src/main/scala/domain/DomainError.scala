package io.github.ntdesmond.serdobot
package domain

import scala.util.control.NoStackTrace

sealed trait DomainError extends NoStackTrace:
  def message: String
  override def getMessage: String = message

case class ParseError(message: String) extends DomainError

case class Business(message: String) extends DomainError
