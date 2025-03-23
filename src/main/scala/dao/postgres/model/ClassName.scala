package io.github.ntdesmond.serdobot
package dao.postgres.model

import domain.ClassNameId
import java.time.LocalDate

case class ClassName(
  id: ClassNameId,
  date: LocalDate,
  number: Int,
  letter: String,
  additionalData: Option[String],
)
