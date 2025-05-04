package io.github.ntdesmond.serdobot
package domain
package schedule

import dao.postgres.PostgresSubtype
import java.time.LocalDate
import java.util.UUID
import zio.prelude.Subtype

case class Subheader(
  id: SubheaderId,
  date: LocalDate,
  timeslotId: Option[TimeSlotId], // None means it's placed before first timeslot
  content: String,
)

object SubheaderId extends Subtype[UUID] with PostgresSubtype[UUID] with MakeRandomUUID
type SubheaderId = SubheaderId.Type
