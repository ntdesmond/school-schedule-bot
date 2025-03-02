package io.github.ntdesmond.serdobot
package service.schedule

import domain.ClassName
import domain.ParseError
import domain.schedule.*
import scalapy.ScalaPyExtensions
import scalapy.modules.camelot.CamelotModule
import scalapy.modules.camelot.PdfCell

import zio.IO
import zio.Task
import zio.ZIO

import java.util.Date

object PdfScheduleParser:
  private def parseTextOnlyRow(cells: List[PdfCell]): String =
    cells.map(_.text).mkString

  private def parseClassNamesRow(cells: List[PdfCell]): IO[
    ParseError,
    List[ClassName],
  ] =
    cells match
      case _ :: classNames =>
        ZIO.foreach(classNames) { cell =>
          ZIO.fromEither(domain.ClassName.fromString(cell.text))
        }

      case Nil => ZIO.fail(ParseError("No class names found"))

  private def parseLessonsRow(
    maybePreviousTimeSlot: Option[TimeSlot],
    classes: List[ClassName],
    lessonsMap: Map[(ClassName, TimeSlot), Lesson],
    row: List[PdfCell],
  ): IO[ParseError, (TimeSlot, Map[(ClassName, TimeSlot), Lesson])] =

    def getLesson(
      map: Map[(ClassName, TimeSlot), Lesson],
      className: ClassName,
      timeSlot: TimeSlot,
    ) =
      ZIO.getOrFailWith(
        ParseError(
          s"Could not find the original cell ($className, $timeSlot) while parsing a span",
        ),
      )(map.get((className, timeSlot)))

    def getLessonFromLeft(
      currentLessonsMap: Map[(ClassName, TimeSlot), Lesson],
      maybePreviousClassName: Option[ClassName],
      timeSlot: TimeSlot,
    ) =
      for
        previousClassName <-
          ZIO.getOrFailWith(
            ParseError("First cell cannot be a horizontal span"),
          )(maybePreviousClassName)
        lesson <- getLesson(currentLessonsMap, previousClassName, timeSlot)
      yield lesson

    def getLessonFromTop(className: ClassName) =
      for
        previousTimeSlot <-
          ZIO.getOrFailWith(
            ParseError("First row cannot contain a vertical span"),
          )(maybePreviousTimeSlot)
        lesson <- getLesson(lessonsMap, className, previousTimeSlot)
      yield lesson

    def parseLessons(timeSlot: TimeSlot, lessons: List[PdfCell]) =
      ZIO
        .foldLeft(lessons.zip(classes))((Option.empty[ClassName], lessonsMap)) {
          case ((previousClassName, lessonsMap), (lessonCell, className)) =>
            {
              (lessonCell.left, lessonCell.top) match
                case (true, true) => Lesson(lessonCell.text)
                case (false, _) => getLessonFromLeft(
                    lessonsMap,
                    previousClassName,
                    timeSlot,
                  )
                case (true, false) => getLessonFromTop(className)
            }.map {
              lesson =>
                (
                  Some(className),
                  lessonsMap.updated((className, timeSlot), lesson),
                )
            }
        }
        .map(_._2)

    row match
      case timeCell :: lessons =>
        for
          timeSlot   <- ZIO.fromEither(TimeSlot.fromString(timeCell.text))
          updatedMap <- parseLessons(timeSlot, lessons)
        yield (timeSlot, updatedMap)
      case Nil => ZIO.fail(ParseError("No lessons in a table"))

  private def parseTable(table: List[List[PdfCell]]): IO[
    ParseError,
    (Option[String], Map[(ClassName, TimeSlot), Lesson]),
  ] =
    table match
      case head :: rest =>
        for
          headerOrClassNames <-
            parseClassNamesRow(head)
              .asRight
              .orElseSucceed(Left(parseTextOnlyRow(head)))
          (_, _, lessons) <-
            ZIO.foldLeft(rest)(
              (
                headerOrClassNames.toOption,
                Option.empty[TimeSlot],
                Map.empty[(ClassName, TimeSlot), Lesson],
              ),
            ) {
              case ((classNames, previousTimeSlot, lessons), row) =>
                classNames match
                  case None => parseClassNamesRow(row).map { classes =>
                      (Some(classes), None, lessons)
                    }
                  case Some(classes) =>
                    parseLessonsRow(previousTimeSlot, classes, lessons, row)
                      .map { case (timeSlot, updatedLessons) =>
                        (classNames, Some(timeSlot), updatedLessons)
                      }
                      .orElseSucceed((None, None, lessons))
            }
        yield (headerOrClassNames.left.toOption, lessons)
      case _ => ZIO.fail(ParseError("Table is empty"))

  private def parseTables: List[List[List[PdfCell]]] => IO[
    domain.ParseError,
    (String, List[ClassSchedule], List[TimeSlot]),
  ] = {
    case firstTable :: rest =>
      for
        (maybeHeader, lessons) <- parseTable(firstTable)
        lessons <-
          ZIO.foldLeft(rest)(lessons)((acc, table) =>
            parseTable(table).map { case (_, newLessons) =>
              acc ++ newLessons
            },
          )
        classSchedules = lessons
          .groupMap(_._1._1)(_._2)
          .map { (className, lessons) =>
            ClassSchedule(className, lessons.toList)
          }
          .toList
        dayInfo <- ZIO.getOrFailWith(
          ParseError("Table header is empty"),
        )(maybeHeader)
      yield (
        dayInfo,
        classSchedules,
        lessons.keys.map(_._2).toSet.toList,
      )
    case _ => ZIO.fail(ParseError("No tables found in the file"))
  }

  def parseFile(
    date: Date,
    path: String,
  ): Task[DaySchedule] =
    ScalaPyExtensions
      .attemptWithStderr(
        CamelotModule
          .read_pdf(path, line_scale = 100, strip_text = "\n")
          .toList
          .map(_.cells),
      )
      .flatMap { case (result, stderr) => ZIO.logWarning(stderr).as(result) }
      .flatMap(parseTables)
      .map { case (dayInfo, classSchedules, timeSlots) =>
        DaySchedule(date, dayInfo, classSchedules, timeSlots)
      }
