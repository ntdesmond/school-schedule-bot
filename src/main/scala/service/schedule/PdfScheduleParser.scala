package io.github.ntdesmond.serdobot
package service.schedule

import domain.ClassName
import domain.ClassNameId
import domain.DomainError
import domain.ParseError
import domain.schedule.*
import java.time.LocalDate
import scalapy.ScalaPyExtensions
import scalapy.modules.camelot.CamelotModule
import scalapy.modules.camelot.PdfCell
import zio.IO
import zio.Task
import zio.ZIO

object PdfScheduleParser:
  private def parseTextOnlyRow(cells: List[PdfCell]): String =
    cells.map(_.text).mkString

  private def parseClassNamesRow(
    date: LocalDate,
    cells: List[PdfCell],
  ): IO[ParseError, List[ClassName]] =
    cells match
      case _ :: classNames =>
        ZIO.foreach(classNames) { cell =>
          ClassNameId.makeRandom().map(domain.ClassName.fromString(_, date, cell.text)).absolve
        }

      case Nil => ZIO.fail(ParseError("No class names found"))

  private def parseLessonsRow(
    date: LocalDate,
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
                case (true, true) => LessonId
                    .makeRandom()
                    .map { id =>
                      LessonName
                        .fromString(lessonCell.text)
                        .map { name =>
                          Lesson(id, name, Set(timeSlot.id), Set(className.id))
                        }
                    }
                case (false, _) =>
                  ZIO
                    .getOrFailWith(
                      ParseError(s"No lesson found to the left of $className"),
                    )(currentRowLessons.headOption)
                    .map(_.map(_.appendClassNameId(className.id)))
                case (true, false) =>
                  ZIO
                    .getOrFailWith(
                      ParseError("No lesson found above the current"),
                    )(lessonFromTop)
                    .map(_.appendTimeSlot(timeSlot.id))
                    .asSome
            }.map(_ :: currentRowLessons)
        }
        .map(_.reverse)

    pdfCells match
      case timeCell :: lessons =>
        for
          id            <- TimeSlotId.makeRandom()
          timeSlot      <- ZIO.fromEither(TimeSlot.fromString(id, date, timeCell.text))
          parsedLessons <- parseLessons(timeSlot, lessons)
        yield (timeSlot, parsedLessons)
      case Nil => ZIO.fail(ParseError("No lessons in a row"))

  private def parseTable(date: LocalDate, table: List[List[PdfCell]]): IO[
    DomainError,
    (
      Option[String],        // Potential header text from the first row if not class names
      Vector[TimeSlot],      // Timeslots in order
      Map[LessonId, Lesson], // All parsed lessons
      Set[ClassName],        // All parsed class names
      Set[Subheader],        // All parsed subheaders
    ),
  ] =
    table match
      case head :: rest =>
        for
          // Try parsing the first row as class names, otherwise treat it as header text
          headerOrClassNames <-
            parseClassNamesRow(date, head).asRight.orElseSucceed(Left(parseTextOnlyRow(head)))

          initialState = (
            headerOrClassNames.toOption,        // maybeClassNames: Option[List[ClassName]]
            Option.empty[List[Option[Lesson]]], // previousRowLessons: Option[List[Option[Lesson]]]
            Option.empty[TimeSlotId],           // lastTimeSlotId: Option[TimeSlotId]
            Vector.empty[TimeSlot],             // timeslots: Vector[TimeSlot]
            Map.empty[LessonId, Lesson],        // lessons: Map[LessonId, Lesson]
            Set.empty[ClassName],               // allClassNames: Set[ClassName]
            Set.empty[Subheader],               // subheaders: Set[Subheader]
          )

          // Fold over the rest of the table rows
          (
            _, // final maybeClassNames (unused)
            _, // final previousRowLessons (unused)
            _, // final lastTimeSlotId (unused)
            timeslots,
            lessons,
            classNames,
            subheaders,
          ) <- ZIO.foldLeft(rest)(initialState) {
            case (
                  (
                    maybeClassNames,
                    previousRowLessons,
                    lastTimeSlotId,
                    timeslots,
                    lessons,
                    allClassNames,
                    subheaders,
                  ),
                  row, // Current row being processed
                ) =>
              maybeClassNames match
                // State 1: Expecting class names row
                case None =>
                  parseClassNamesRow(date, row)
                    .map { parsedClassNames =>
                      // Successfully parsed class names
                      (
                        Some(parsedClassNames),
                        None, // Reset previousRowLessons
                        lastTimeSlotId,
                        timeslots,
                        lessons,
                        allClassNames ++ parsedClassNames, // Add new class names
                        subheaders,
                      )
                    }
                    .orElse {
                      // Failed to parse as class names, treat as text-only
                      val text = parseTextOnlyRow(row)

                      SubheaderId
                        .makeRandom()
                        .map { id =>
                          (
                            None, // Still expecting class names
                            None,
                            lastTimeSlotId,
                            timeslots,
                            lessons,
                            allClassNames,
                            subheaders ++ Option
                              .when(text.nonEmpty)(Subheader(id, date, lastTimeSlotId, text)),
                          )
                        }
                    }

                // State 2: Expecting lessons row
                case Some(currentClassNames) =>
                  parseLessonsRow(date, previousRowLessons, currentClassNames, row)
                    .map { case (timeSlot, rowLessons) =>
                      // Successfully parsed lessons row
                      (
                        Some(currentClassNames), // Remain in lesson parsing state
                        Some(rowLessons),  // Store current lessons as previous for next iteration
                        Some(timeSlot.id), // Update last parsed timeslot ID
                        timeslots :+ timeSlot, // Add new timeslot
                        lessons ++ rowLessons.collect { case Some(lesson) =>
                          lesson.id -> lesson
                        }, // Add new lessons
                        allClassNames,
                        subheaders,
                      )
                    }
                    .catchSome { case _: ParseError =>
                      // Failed to parse as lessons, treat as text-only
                      val text = parseTextOnlyRow(row)

                      SubheaderId
                        .makeRandom()
                        .map { id =>
                          (
                            Some(currentClassNames), // Remain in lesson parsing state
                            None,                    // Reset previousRowLessons
                            lastTimeSlotId,          // Keep the same lastTimeSlotId
                            timeslots,
                            lessons,
                            allClassNames,
                            subheaders ++ Option
                              .when(text.nonEmpty)(Subheader(id, date, lastTimeSlotId, text)),
                          )
                        }
                    }
          }
        // Return the accumulated results, using the potential header text derived initially
        yield (headerOrClassNames.left.toOption, timeslots, lessons, classNames, subheaders)
      case _ => ZIO.fail(ParseError("Table is empty"))

  private def parseTables(
    date: LocalDate,
  ): List[List[List[PdfCell]]] => IO[DomainError, DaySchedule] = {
    case firstTable :: rest =>
      for
        // Parse the first table
        (maybeHeader, initialTimeslots, initialLessons, initialClassNames, initialSubheaders) <-
          parseTable(date, firstTable)

        // Fold over the rest of the tables, accumulating results
        (
          timeslots,
          lessons,
          classNames,
          subheaders,
        ) <-
          ZIO.foldLeft(rest)(
            (initialTimeslots, initialLessons, initialClassNames, initialSubheaders),
          ) { case ((accTimeslots, accLessons, accClassNames, accSubheaders), table) =>
            parseTable(date, table).map {
              case (_, newTimeslots, newLessons, newClassNames, newSubheaders) =>
                (
                  accTimeslots ++ newTimeslots,
                  accLessons ++ newLessons,
                  accClassNames ++ newClassNames,
                  accSubheaders ++ newSubheaders,
                )
            }
          }

        // Ensure a header was found (either from the first row of the first table or explicitly)
        dayInfo <- ZIO.getOrFailWith(
          ParseError("Table header is empty or could not be determined"),
        )(
          maybeHeader,
        ) // Assumes header comes from the first table's first row if it wasn't class names

      // Create the final DaySchedule domain object
      yield DaySchedule.make(date, dayInfo, timeslots, classNames, lessons.values, subheaders)
    case _ => ZIO.fail(ParseError("No tables found in the file"))
  }

  def parseFile(date: LocalDate, path: String): Task[DaySchedule] =
    for
      (cells, stderr) <- ScalaPyExtensions.attemptWithStderr {
        CamelotModule
          .read_pdf(
            path,
            line_scale = 100,
            strip_text = "\n",
            split_text = true,
          )
          .toList
          .map(_.cells)
      }
      _        <- ZIO.logWarning(stderr).when(stderr.nonEmpty)
      schedule <- parseTables(date)(cells)
    yield schedule
