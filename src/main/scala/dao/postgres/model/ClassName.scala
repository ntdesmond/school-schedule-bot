package io.github.ntdesmond.serdobot
package dao.postgres.model

import domain.ClassNameId
import java.util.Date

case class ClassName(
  id: ClassNameId,
  date: Date,
  number: Int,
  letter: String,
  additionalData: Option[String],
)
