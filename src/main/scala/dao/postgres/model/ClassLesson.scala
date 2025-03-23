package io.github.ntdesmond.serdobot
package dao.postgres.model

import domain.ClassNameId
import domain.schedule.LessonId
import java.util.Date

case class ClassLesson(classNameId: ClassNameId, lessonId: LessonId)
