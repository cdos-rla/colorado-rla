/*
 * Colorado RLA System
 */

package us.freeandfair.corla.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.util.List;

import org.apache.poi.ss.usermodel.*;

import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFTable;

import org.openxmlformats.schemas.spreadsheetml.x2006.main.*;

/**
 * Generate a POI Excel workbook containing audit report sheets.
 *
 * Sheets include:
 *
 * - Summary
 */
public class WorkbookWriter {
  /**
   * Font size to use for all cells.
   */
  private static final Short FONT_SIZE = 12;

  /**
   * Font size to use for all cells.
   */
  private static final Short TITLE_FONT_SIZE = 14;


  /**
   * Internal output stream required for writing out the workbook.
   */
  private final ByteArrayOutputStream baos;


  /**
   * one workbook to hold multiple sheets, another way to say Excel file, xlsx
   **/
  private final Workbook        workbook;

  /** regular style **/
  private final CellStyle regStyle;

  /** bold style for headers **/
  private final CellStyle boldStyle;

  /** bold style for headers **/
  private final CellStyle titleStyle;

  /** bold style for headers **/
  private final CellStyle hiddenAccessibilityStyle;


  /**
   * Initializes the AuditReport
   */
  public WorkbookWriter() {
    this.baos = new ByteArrayOutputStream();
    this.workbook = new XSSFWorkbook();
    this.boldStyle = this.workbook.createCellStyle();
    this.regStyle = this.workbook.createCellStyle();
    this.titleStyle = this.workbook.createCellStyle();
    this.hiddenAccessibilityStyle = this.workbook.createCellStyle();

    setStyles();
  }

  private void setStyles() {
    final Font boldFont = workbook.createFont();
    final Font regFont = workbook.createFont();
    final Font titleFont = workbook.createFont();
    final Font hiddenAccessibilityFont = workbook.createFont();

    regFont.setFontHeightInPoints(FONT_SIZE);
    this.regStyle.setFont(regFont);

    boldFont.setFontHeightInPoints(FONT_SIZE);
    boldFont.setBold(true);
    this.boldStyle.setFont(boldFont);

    titleFont.setFontHeightInPoints(TITLE_FONT_SIZE);
    titleFont.setBold(true);
    this.titleStyle.setFont(titleFont);

    hiddenAccessibilityFont.setFontHeightInPoints(FONT_SIZE);
    hiddenAccessibilityFont.setColor(IndexedColors.WHITE.getIndex());
    this.hiddenAccessibilityStyle.setFillBackgroundColor(IndexedColors.WHITE.getIndex());
    this.hiddenAccessibilityStyle.setFont(hiddenAccessibilityFont);
  }

  public void addSheet(final String sheetName, final List<List<String>> rows) {
    String correctSheetName = replaceSheetnameSpecialChars(sheetName);
    XSSFSheet sheet = null;
    sheet = (XSSFSheet) workbook.createSheet(correctSheetName);

    // Remove characters not permitted for excel table names
    String tableName = correctSheetName.replace(" ", "_").replace("(", "_").replace(")", "_").replace("-", "");
    createTable(workbook, sheet, tableName, 1, 0, rows.size(), rows.get(0).size() - 1);

    // Get total columns from second row with headers
    hideUnusedRowsAndColumns(sheet, rows.get(1).size());

    for (int i = 0; i < rows.size(); i++) {
      final Row poiRow = sheet.createRow(i + 1);
      final List<String> dataRow = rows.get(i);

      for (int j = 0; j < dataRow.size(); j++) {

        if (i == 0) {
          // First row of sheet is title
          createTitleRow(sheet, dataRow.size());
        }

        final Cell cell = poiRow.createCell(j);
        cell.setCellValue(dataRow.get(j));

        if (i == 0) {
          // Embolden header rows
          cell.setCellStyle(boldStyle);
        } else {
          cell.setCellStyle(regStyle);
        }
      }
    }
    // format column widths
    for (int i = 0; i < rows.get(1).size(); i++) {
      sheet.autoSizeColumn(i);
    }

    markEmptyCellsAsNoData(sheet, rows.get(1).size(), this.hiddenAccessibilityStyle);
    //Create "end of document" row at bottom for accessibility
    createEndingRow(sheet, rows.size() + 1, this.hiddenAccessibilityStyle);
  }

  public String replaceSheetnameSpecialChars(String correctSheetName) {
    return correctSheetName.replaceAll("[/:]", " ");
  }

  public static void hideUnusedRowsAndColumns(XSSFSheet sheet, int totalColumns) {
    //Hide unused rows
    CTWorksheet ctWorksheet = sheet.getCTWorksheet();
    CTSheetFormatPr ctSheetFormatPr = ctWorksheet.getSheetFormatPr();
    if (ctSheetFormatPr == null) {
      ctSheetFormatPr = ctWorksheet.addNewSheetFormatPr();
    }
    ctSheetFormatPr.setZeroHeight(true);

    // Hide unused columns
    CTCols cols = ctWorksheet.getColsArray(0);
    CTCol ctCol = cols.addNewCol();
    //Column after data
    ctCol.setMin(totalColumns + 1);
    // Get max column width from header row
    // Max column amount for XSSF
    ctCol.setMax(16384);
    ctCol.setHidden(true);
  }

  private void createTitleRow(XSSFSheet sheet, int rowLength) {
    // Use A1 cell for table title
    final Row titleRow = sheet.createRow(0);
    final Cell titleCell = titleRow.createCell(0);
    titleCell.setCellValue(sheet.getSheetName());
    titleCell.setCellStyle(this.titleStyle);
  }

  public static void createEndingRow(XSSFSheet sheet, int endingRowIndex, CellStyle hiddenStyle) {
    // Use A1 cell for table title
    final Row endRow = sheet.createRow(endingRowIndex);
    final Cell titleCell = endRow.createCell(0);
    titleCell.setCellValue("End of worksheet");
    titleCell.setCellStyle(hiddenStyle);
  }

  public static void markEmptyCellsAsNoData(XSSFSheet sheet, int sheetWidth, CellStyle hiddenStyle) {
    for (int rowNum = sheet.getFirstRowNum(); rowNum <= sheet.getLastRowNum(); rowNum++) {
      Row row = sheet.getRow(rowNum);
      if (row == null) continue;
      for (int cn = 0; cn < sheetWidth; cn++) {
        // Policy ensures it returns a blank cell object instead of null if the cell is missing
        Cell cell = row.getCell(cn, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);

        if (cell.getCellType() == CellType.BLANK) {
          cell.setCellValue("No Data");
          cell.setCellStyle(hiddenStyle);
        }
      }
    }
  }

  public static void createTable(Workbook workbook, XSSFSheet sheet, String tableName, int topMostRow, int leftMostColumn, int bottomMostRow, int rightMostColumn) {
    // Define the area for the table
    AreaReference tableArea = workbook.getCreationHelper().createAreaReference(
            new CellReference(topMostRow, leftMostColumn),
            new CellReference(bottomMostRow, rightMostColumn));
    XSSFTable table = sheet.createTable(tableArea);

    table.getCTTable().setDisplayName(tableName);
    // Enable filtering from table headers
    table.getCTTable().addNewAutoFilter().setRef(tableArea.formatAsString());
  }

  /**
   * Generate the byte-array representation of this POI workbook.
   *
   * @return the Excel representation of this report
   * @exception IOException if the report cannot be generated.
   */
  public byte[] write() throws IOException {
    this.workbook.write(this.baos);
    this.workbook.close();

    return this.baos.toByteArray();
  }
}
