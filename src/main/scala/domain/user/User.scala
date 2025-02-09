package io.github.ntdesmond.serdobot
package domain.user

import zio.prelude.Subtype

case class User(id: UserId)

object UserId extends Subtype[Int]
type UserId = UserId.Type
