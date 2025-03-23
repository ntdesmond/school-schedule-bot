package io.github.ntdesmond.serdobot
package domain
package schedule

case class ClassSchedule(className: ClassName, lessons: List[(TimeSlot, Lesson)]):
  def asFormattedString: String =
    s"""
       |className = ${className.asFormattedString}
       |${
        lessons
          .map { case (timeslot, lesson) =>
            List(
              timeslot.start.toString + "-" + timeslot.end.toString,
              lesson.name,
              lesson.id,
            ).mkString(" | ")
          }
          .mkString("\n")
      }""".stripMargin
