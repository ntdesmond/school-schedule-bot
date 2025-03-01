package io.github.ntdesmond.serdobot
package domain
package schedule

import zio.prelude.Subtype
import zio.UIO
import zio.Random

import java.util.UUID

case class Lesson(id: LessonId, name: String)

object Lesson:
  def apply(name: String): UIO[Lesson] =
    val cleanName = """\s+""".r.replaceAllIn(name, " ")
    Random.nextUUID.map(uuid => Lesson(LessonId(uuid), cleanName))

object LessonId extends Subtype[UUID]
type LessonId = LessonId.Type
