package io.github.ntdesmond.serdobot
package service.schedule

import domain.ClassName
import domain.DomainError
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

  private def parseClassNamesRow(cells: List[PdfCell]): IO[ParseError, List[ClassName]] =
    cells match
      case _ :: classNames =>
        ZIO.foreach(classNames) { cell =>
          ZIO.fromEither(domain.ClassName.fromString(cell.text))
        }

      case Nil => ZIO.fail(ParseError("No class names found"))

  private def parseLessonsRow(
    maybePreviousRow: Option[List[Option[Lesson]]],
    classes: List[ClassName],
    pdfCells: List[PdfCell],
  ): IO[DomainError, (TimeSlot, List[Option[Lesson]])] =
    def parseLessons(timeSlot: TimeSlot, lessons: List[PdfCell]) =
      val iterable = maybePreviousRow match
        case Some(row) => lessons.zip(classes).zip(row)
        case None      => lessons.zip(classes).map((_, None))
      ZIO
        .foldLeft(iterable)(List.empty[Option[Lesson]]) {
          case (currentRowLessons, ((lessonCell, className), lessonFromTop)) =>
            {
              (lessonCell.left, lessonCell.top) match
                case (true, true) => Lesson(lessonCell.text, timeSlot, Set(className))
                case (false, _) =>
                  ZIO
                    .getOrFailWith(
                      ParseError(s"No lesson found to the left of $className"),
                    )(currentRowLessons.headOption)
                    .map(_.map(_.appendClassName(className)))
                case (true, false) =>
                  ZIO
                    .getOrFailWith(
                      ParseError("No lesson found above the current"),
                    )(lessonFromTop)
                    .map(_.extendTimeSlot(timeSlot))
                    .absolve
                    .asSome
            }.map(_ :: currentRowLessons)
        }
        .map(_.reverse)

    pdfCells match
      case timeCell :: lessons =>
        for
          timeSlot      <- ZIO.fromEither(TimeSlot.fromString(timeCell.text))
          parsedLessons <- parseLessons(timeSlot, lessons)
        yield (timeSlot, parsedLessons)
      case Nil => ZIO.fail(ParseError("No lessons in a row"))

  private def parseTable(table: List[List[PdfCell]]): IO[
    DomainError,
    (Option[String], Set[TimeSlot], Map[LessonId, Lesson]),
  ] =
    table match
      case head :: rest =>
        for
          headerOrClassNames <-
            parseClassNamesRow(head).asRight.orElseSucceed(Left(parseTextOnlyRow(head)))
          (_, _, timeslots, lessons) <-
            ZIO.foldLeft(rest)(
              (
                headerOrClassNames.toOption,
                Option.empty[List[Option[Lesson]]],
                Set.empty[TimeSlot],
                Map.empty[LessonId, Lesson],
              ),
            ) {
              case ((maybeClassNames, previousRowLessons, timeslots, lessons), row) =>
                maybeClassNames match
                  case None => parseClassNamesRow(row).map { classNames =>
                      (Some(classNames), None, timeslots, lessons)
                    }
                  case Some(classNames) =>
                    parseLessonsRow(previousRowLessons, classNames, row)
                      .map { case (timeSlot, rowLessons) =>
                        (
                          maybeClassNames,
                          Some(rowLessons),
                          timeslots + timeSlot,
                          lessons ++ rowLessons.collect { case Some(lesson) => lesson.id -> lesson },
                        )
                      }
                      .catchSome {
                        case _: ParseError => ZIO.succeed((None, None, timeslots, lessons))
                      }
            }
        yield (headerOrClassNames.left.toOption, timeslots, lessons)
      case _ => ZIO.fail(ParseError("Table is empty"))

  private def parseTables: List[List[List[PdfCell]]] => IO[
    DomainError,
    (String, Set[TimeSlot], Set[Lesson]),
  ] = {
    case firstTable :: rest =>
      for
        (maybeHeader, timeslots, lessons) <- parseTable(firstTable)
        (timeslots, lessons) <-
          ZIO.foldLeft(rest)((timeslots, lessons)) {
            case ((accTimeslots, accLessons), table) =>
              parseTable(table).map { case (_, newTimeslots, newLessons) =>
                (accTimeslots ++ newTimeslots, accLessons ++ newLessons)
              }
          }
        dayInfo <- ZIO.getOrFailWith(
          ParseError("Table header is empty"),
        )(maybeHeader)
      yield (dayInfo, timeslots, lessons.values.toSet)
    case _ => ZIO.fail(ParseError("No tables found in the file"))
  }

  def parseFile(
    date: Date,
    path: String,
  ): Task[DaySchedule] =
    ScalaPyExtensions
      .attemptWithStderr(
        CamelotModule
          .read_pdf(
            path,
            line_scale = 100,
            strip_text = "\n",
            split_text = true,
          )
          .toList
          .map(_.cells),
      )
      .flatMap { case (result, stderr) =>
        ZIO.logWarning(stderr).when(stderr.nonEmpty).as(result)
      }
      .flatMap(parseTables)
      .map { case (dayInfo, timeslots, lessons) =>
        DaySchedule(date, dayInfo, timeslots, lessons)
      }
