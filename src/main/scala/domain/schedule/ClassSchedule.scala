package io.github.ntdesmond.serdobot
package domain
package schedule

case class ClassSchedule(className: ClassName, lessons: List[Lesson]):
  def asFormattedString: String =
    s"""
       |className = ${className.asFormattedString}
       |${
        lessons
          .map { lesson =>
            List(
              lesson.timeSlot.start.toString + "-" + lesson.timeSlot.end.toString,
              lesson.name,
              lesson.id,
            ).mkString(" | ")
          }
          .mkString("\n")
      }""".stripMargin
