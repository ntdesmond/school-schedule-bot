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
    maybePreviousRow: Option[List[Lesson]],
    classes: List[ClassName],
    row: List[PdfCell],
  ): IO[ParseError, (TimeSlot, List[Lesson])] =
    def parseLessons(timeSlot: TimeSlot, lessons: List[PdfCell]) =
      val iterable = maybePreviousRow match
        case Some(row) => lessons.zip(classes).zip(row.map(Some.apply))
        case None      => lessons.zip(classes).map((_, None))
      ZIO
        .foldLeft(iterable)(List.empty[Lesson]) {
          case (
                currentRowLessons,
                ((lessonCell, className), lessonFromTop),
              ) =>
            {
              (lessonCell.left, lessonCell.top) match
                case (true, true) => Lesson(lessonCell.text)
                case (false, _) =>
                  ZIO.getOrFailWith(
                    ParseError(
                      s"No lesson found to the left of $className",
                    ),
                  )(currentRowLessons.headOption)
                case (true, false) =>
                  ZIO.getOrFailWith(
                    ParseError("No lesson found above the current"),
                  )(lessonFromTop)
            }.map(_ :: currentRowLessons)
        }
        .map(_.reverse)

    row match
      case timeCell :: lessons =>
        for
          timeSlot      <- ZIO.fromEither(TimeSlot.fromString(timeCell.text))
          parsedLessons <- parseLessons(timeSlot, lessons)
        yield (timeSlot, parsedLessons)
      case Nil => ZIO.fail(ParseError("No lessons in a row"))

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
                Option.empty[List[Lesson]],
                Map.empty[(ClassName, TimeSlot), Lesson],
              ),
            ) {
              case ((maybeClassNames, previousRowLessons, lessons), row) =>
                maybeClassNames match
                  case None => parseClassNamesRow(row).map { classNames =>
                      (Some(classNames), None, lessons)
                    }
                  case Some(classNames) =>
                    parseLessonsRow(previousRowLessons, classNames, row)
                      .map { case (timeSlot, rowLessons) =>
                        (
                          maybeClassNames,
                          Some(rowLessons),
                          lessons ++
                            classNames
                              .zip(rowLessons)
                              .map {
                                case (className, lesson) =>
                                  (className, timeSlot) -> lesson
                              },
                        )
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
          .read_pdf(
            path,
            line_scale = 100,
            strip_text = "\n",
            split_text = true,
          )
          .toList
          .map(_.cells),
      )
      .flatMap { case (result, stderr) => ZIO.logWarning(stderr).as(result) }
      .flatMap(parseTables)
      .map { case (dayInfo, classSchedules, timeSlots) =>
        DaySchedule(date, dayInfo, classSchedules, timeSlots)
      }
