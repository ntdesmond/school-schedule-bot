package io.github.ntdesmond.serdobot
package service.schedule

import scala.language.dynamics
import me.shadaj.scalapy.py

@py.native
object CamelotModule extends py.StaticModule("camelot"):
  private type PyOption[T] = py.|[T, Unit]

  /** @param filepath
    *   Path of the PDF file.
    * @param pages
    *   Comma-separated page numbers. Example: ‘1,3,4’ or ‘1,4-end’ or ‘all’.
    * @param password
    *   Password for decryption.
    * @param flavor
    *   The parsing method to use (Only ‘lattice’ supported in this overload).
    * @param suppress_stdout
    *   Whether to print all logs and warnings.
    * @param parallel
    *   Whether to process pages in parallel using all available cpu cores.
    * @param table_areas
    *   List of table area strings of the form x1,y1,x2,y2 where (x1, y1) ->
    *   left-top and (x2, y2) -> right-bottom in PDF coordinate space.
    * @param split_text
    *   Split text that spans across multiple cells.
    * @param flag_size
    *   Flag text based on font size. Useful to detect super/subscripts. Adds
    *   <s></s> around flagged text.
    * @param strip_text
    *   Characters that should be stripped from a string before assigning it to
    *   a cell.
    * @param process_background
    *   Process background lines.
    * @param line_scale
    *   Line size scaling factor. The larger the value the smaller the detected
    *   lines. Making it very large will lead to text being detected as lines.
    * @param copy_text
    *   {‘h’, ‘v’} Direction in which text in a spanning cell will be copied
    *   over.
    * @param shift_text
    *   {‘l’, ‘r’, ‘t’, ‘b’} Direction in which text in a spanning cell will
    *   flow.
    * @param line_tol
    *   Tolerance parameter used to merge close vertical and horizontal lines.
    * @param joint_tol
    *   Tolerance parameter used to decide whether the detected lines and points
    *   lie close to each other.
    * @param threshold_blocksize
    *   (OpenCV) Size of a pixel neighborhood that is used to calculate a
    *   threshold value for the pixel: 3, 5, 7, and so on.
    * @param threshold_constant
    *   (OpenCV) Constant subtracted from the mean or weighted mean. Normally,
    *   it is positive but may be zero or negative as well.
    * @param iterations
    *   (OpenCV) Number of times for erosion/dilation is applied.
    * @param backend
    *   The backend to use for converting the PDF to an image so it can be
    *   processed by OpenCV.
    * @param use_fallback
    *   Fallback to another backend if unavailable, by default True
    * @param resolution
    *   Resolution used for PDF to PNG conversion.
    * @return
    *   `TableList` with all found tables
    */
  def read_pdf(
    filepath: String,
    pages: String = "1",
    password: PyOption[String] = None,
    flavor: "lattice" = "lattice",
    suppress_stdout: Boolean = false,
    parallel: Boolean = false,
    table_areas: PyOption[py.Any] = None,
    split_text: Boolean = false,
    flag_size: Boolean = false,
    strip_text: String = "",
    process_background: Boolean = false,
    line_scale: Int = 40,
    copy_text: PyOption[py.Any] = None,
    shift_text: PyOption[py.Any] = Some(List("l", "t")),
    line_tol: Int = 2,
    joint_tol: Int = 2,
    threshold_blocksize: Int = 15,
    threshold_constant: Int = -1,
    iterations: Int = 0,
    backend: String = "pdfium",
    use_fallback: Boolean = true,
    resolution: Int = 300,
  ): TableList = py.nativeNamed
