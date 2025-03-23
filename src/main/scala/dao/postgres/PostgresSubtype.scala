package io.github.ntdesmond.serdobot
package dao.postgres

import io.getquill.MappedEncoding
import zio.prelude.Subtype

trait PostgresSubtype[A]:
  self: Subtype[A] =>
  given MappedEncoding[self.Type, A] = MappedEncoding(self.unwrap)
  given MappedEncoding[A, self.Type] = MappedEncoding(self.wrap)
