package io.github.ntdesmond.serdobot
package domain
package schedule

import zio.prelude.Subtype

import java.util.UUID

case class Lesson(id: LessonId, name: String)

object LessonId extends Subtype[UUID]
type LessonId = LessonId.Type
