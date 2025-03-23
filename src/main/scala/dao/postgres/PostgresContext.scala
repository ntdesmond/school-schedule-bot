package io.github.ntdesmond.serdobot
package dao.postgres

import domain.ClassName
import domain.schedule.TimeSlot
import io.getquill.JsonbValue
import io.getquill.MappedEncoding
import io.getquill.PostgresZioJdbcContext
import io.getquill.SnakeCase
import java.time.LocalTime
import scala.language.adhocExtensions

object PostgresContext extends PostgresZioJdbcContext[SnakeCase](SnakeCase):
  implicit def arrayJsonbEncoder[A, Col <: Seq[JsonbValue[A]]]: Encoder[Col] =
    arrayRawEncoder[JsonbValue[A], Col]("jsonb")

  implicit def arrayJsonbDecoder[A, Col <: Seq[JsonbValue[A]]](
    implicit bf: CBF[JsonbValue[A], Col],
  ): Decoder[Col] =
    arrayRawDecoder[JsonbValue[A], Col]
