package io.github.ntdesmond.serdobot
package domain

import java.util.UUID
import zio.Random
import zio.UIO
import zio.prelude.Subtype

trait MakeRandomUUID:
  self: Subtype[UUID] =>
  def makeRandom(): UIO[Type] = Random.nextUUID.map(self.wrap)
