package io.github.ntdesmond.serdobot
package dao.postgres.model

import domain.ClassNameId
import domain.schedule.LessonId
import domain.schedule.TimeSlotId
import java.util.Date

case class ClassTimeLesson(classNameId: ClassNameId, timeslotId: TimeSlotId, lessonId: LessonId)
