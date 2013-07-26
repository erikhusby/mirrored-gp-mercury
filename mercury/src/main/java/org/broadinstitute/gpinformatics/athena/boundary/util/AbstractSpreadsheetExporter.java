package org.broadinstitute.gpinformatics.athena.boundary.util;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.text.Format;
import java.util.Date;
import java.util.Iterator;

/**
 * Wrapper for all exporters so that the writer and exporter can share some members.
 */
public abstract class AbstractSpreadsheetExporter {

    public static final Format DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd-HH:mm:ss");

    private final Workbook workbook;

    private final CellStyle fixedHeaderStyle;
    private final CellStyle priceItemProductHeaderStyle;
    private final CellStyle billedAmountsHeaderStyle;
    private final CellStyle billedAmountStyle;
    private final CellStyle preambleStyle;
    private final CellStyle errorMessageStyle;
    private final CellStyle dateStyle;
    private final CellStyle riskStyle;
    private final CellStyle abandonedStyle;

    private final SpreadSheetWriter writer = new SpreadSheetWriter();

    public AbstractSpreadsheetExporter() {
        // SXSSFWorkbook is used to support very large spreadsheets.  SXSSF writes 100 rows at a time to a
        // temporary file, which is then copied into the output stream when all spreadsheet data has been written.
        workbook = new SXSSFWorkbook();
        fixedHeaderStyle = buildHeaderStyle(workbook, IndexedColors.LIGHT_CORNFLOWER_BLUE);
        priceItemProductHeaderStyle = buildHeaderStyle(workbook, IndexedColors.GREY_25_PERCENT);
        billedAmountsHeaderStyle = buildHeaderStyle(workbook, IndexedColors.LIGHT_YELLOW);
        billedAmountStyle = buildHeaderStyle(workbook, IndexedColors.TAN);
        preambleStyle = buildPreambleStyle(workbook);
        errorMessageStyle = buildColorStyle(workbook, IndexedColors.RED, IndexedColors.BLACK);
        dateStyle = buildDateStyle(workbook);
        riskStyle = buildColorStyle(workbook, IndexedColors.YELLOW, IndexedColors.BLACK);
        abandonedStyle = buildColorCellStyle(workbook, IndexedColors.ROSE);
    }

    protected SpreadSheetWriter getWriter() {
        return writer;
    }

    protected CellStyle getFixedHeaderStyle() {
        return fixedHeaderStyle;
    }

    protected CellStyle getPriceItemProductHeaderStyle() {
        return priceItemProductHeaderStyle;
    }

    protected CellStyle getRiskStyle() {
        return riskStyle;
    }
    protected CellStyle getAbandonedStyle() {
        return abandonedStyle;
    }

    protected CellStyle getDateStyle() {
        return dateStyle;
    }

    protected CellStyle getErrorMessageStyle() {
        return errorMessageStyle;
    }

    protected CellStyle getBilledAmountsHeaderStyle() {
        return billedAmountsHeaderStyle;
    }

    protected CellStyle getBilledAmountStyle() {
        return billedAmountStyle;
    }

    protected Workbook getWorkbook() {
        return workbook;
    }

    protected CellStyle buildHeaderStyle(Workbook wb, IndexedColors indexedColors) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(indexedColors.getIndex());
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setAlignment(CellStyle.ALIGN_CENTER);
        Font headerFont = wb.createFont();
        headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
        style.setFont(headerFont);
        return style;
    }

    protected CellStyle buildPreambleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font headerFont = wb.createFont();
        headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
        style.setFont(headerFont);
        return style;
    }

    protected CellStyle buildDateStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        CreationHelper createHelper = wb.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("m/d/yy"));
        return style;
    }

    protected CellStyle buildColorStyle(Workbook wb, IndexedColors foregroundColor, IndexedColors fontColor) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(foregroundColor.getIndex());
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setAlignment(CellStyle.ALIGN_LEFT);
        style.setWrapText(true);
        Font font = wb.createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        font.setColor(fontColor.getIndex());
        style.setFont(font);
        return style;
    }

    protected CellStyle buildColorCellStyle(Workbook wb, IndexedColors foregroundColor) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(foregroundColor.getIndex());
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        return style;
    }

    public class SpreadSheetWriter {
        private Row currentRow;
        private Cell currentCell;
        private int rowNum;
        private int cellNum;
        private Sheet currentSheet;

        public void createSheet(String sheetName) {
            currentSheet = workbook.createSheet(sheetName);
            rowNum = 0;
        }

        public void nextRow() {
            currentRow = currentSheet.createRow(rowNum++);
            cellNum = 0;
        }

        public void nextCell() {
            currentCell = currentRow.createCell(cellNum++);
        }

        public void nextCell(int colspan) {
            currentCell = currentRow.createCell(cellNum);
            currentSheet.addMergedRegion(new CellRangeAddress(
                    currentRow.getRowNum(),
                    currentRow.getRowNum(),
                    cellNum,
                    cellNum + colspan - 1
            ));
            cellNum += colspan;
        }

        public void setColumnWidth(int columnWidth) {
            currentSheet.setColumnWidth(currentCell.getColumnIndex(), columnWidth);
        }

        public void createFreezePane(int colSplit, int rowSplit) {
            currentSheet.createFreezePane(colSplit, rowSplit);
        }

        public void writePreamble(String preamble) {
            nextRow();
            writeCell(preamble, preambleStyle);
            nextRow();
        }

        public void writeCell(String value, CellStyle style) {
            nextCell();
            currentCell.setCellValue(value);
            currentCell.setCellStyle(style);
        }

        public void writeCell(String value, int colspan, CellStyle style) {
            nextCell(colspan);
            currentCell.setCellValue(value);
            currentCell.setCellStyle(style);
        }

        public void writeCell(String value) {
            nextCell();
            currentCell.setCellValue(value);
        }

        public void writeCell(double value) {
            nextCell();
            currentCell.setCellValue(value);
        }

        public void writeCell(double value, CellStyle cellStyle) {
            nextCell();
            currentCell.setCellValue(value);
            currentCell.setCellStyle(cellStyle);
        }

        public void writeCell(Date value, CellStyle cellStyle) {
            nextCell();
            if (value != null) {
                currentCell.setCellValue(value);
            }
            currentCell.setCellStyle(cellStyle);
        }

        public void setRowStyle(CellStyle style) {
            // Using currentRow.setRowStyle() sets the style for all cells, including unused ones.
            // Instead, only set the style for the cells that have content.
            Iterator<Cell> cells = currentRow.cellIterator();
            while (cells.hasNext()) {
                cells.next().setCellStyle(style);
            }
        }
    }
}
