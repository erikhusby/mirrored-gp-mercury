package org.broadinstitute.gpinformatics.athena.boundary.util;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;

import java.math.BigInteger;
import java.text.Format;
import java.util.Date;
import java.util.Iterator;

/**
 * Wrapper for all exporters so that the writer and exporter can share some members.
 *
 * @param <T>
 */
public abstract class AbstractSpreadsheetExporter<T extends AbstractSpreadsheetExporter.SpreadSheetWriter> {

    public static final Format DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd-HH:mm:ss");

    private final Workbook workbook;

    private final CellStyle fixedHeaderStyle;
    private final CellStyle billedAmountsHeaderStyle;
    private final CellStyle billedAmountStyle;
    private final CellStyle preambleStyle;
    private final CellStyle errorMessageStyle;
    private final CellStyle dateStyle;
    private final CellStyle riskStyle;
    private final CellStyle abandonedStyle;
    private final CellStyle percentageStyle;
    private final CellStyle hyperlinkStyle;

    private final T writer;

    protected AbstractSpreadsheetExporter(T writer) {
        this.writer = writer;
        // SXSSFWorkbook is used to support very large spreadsheets.  SXSSF writes 100 rows at a time to a
        // temporary file, which is then copied into the output stream when all spreadsheet data has been written.
        workbook = new SXSSFWorkbook();
        writer.setWorkbook(workbook);
        fixedHeaderStyle = buildHeaderStyle(workbook, IndexedColors.LIGHT_CORNFLOWER_BLUE);
        billedAmountsHeaderStyle = buildHeaderStyle(workbook, IndexedColors.LIGHT_YELLOW);
        billedAmountStyle = buildHeaderStyle(workbook, IndexedColors.TAN);
        preambleStyle = buildPreambleStyle(workbook);
        errorMessageStyle = buildColorStyle(workbook, IndexedColors.RED, IndexedColors.BLACK);
        dateStyle = buildDateStyle(workbook);
        riskStyle = buildColorStyle(workbook, IndexedColors.YELLOW, IndexedColors.BLACK);
        abandonedStyle = buildColorCellStyle(workbook, IndexedColors.ROSE);
        percentageStyle = buildPercentageCellStyle(workbook);
        hyperlinkStyle = buildHyperlinkStyle(workbook);
    }

    protected T getWriter() {
        return writer;
    }

    protected CellStyle getFixedHeaderStyle() {
        return fixedHeaderStyle;
    }

    /**
     * Creates a cell style for a header with text wrapping and the given background color.
     *
     * @param color    the background color for the style
     * @return the cell style
     */
    protected CellStyle getWrappedHeaderStyle(XSSFColor color) {
        CellStyle style = buildWrappedHeaderStyle();
        ((XSSFCellStyle) style).setFillForegroundColor(color);
        return style;
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

    protected CellStyle getPercentageStyle() { return percentageStyle; }

    public CellStyle getHyperlinkStyle() {
        return hyperlinkStyle;
    }

    protected Workbook getWorkbook() {
        return workbook;
    }

    public static CellStyle buildHeaderStyle(Workbook wb, IndexedColors indexedColors) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(indexedColors.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        style.setFont(headerFont);
        return style;
    }

    /**
     * Builds a cell style for a header with text wrapping.
     */
    protected CellStyle buildWrappedHeaderStyle() {
        CellStyle style = buildHeaderStyle(workbook, IndexedColors.WHITE);
        style.setWrapText(true);
        return style;
    }

    protected static CellStyle buildPreambleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
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
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setWrapText(false);
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(fontColor.getIndex());
        style.setFont(font);
        return style;
    }

    protected CellStyle buildColorCellStyle(Workbook wb, IndexedColors foregroundColor) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(foregroundColor.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    protected CellStyle buildPercentageCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("0.000%"));
        return style;
    }

    private static CellStyle buildHyperlinkStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setColor(IndexedColors.BLUE.getIndex());
        font.setUnderline(Font.U_SINGLE);
        style.setFont(font);
        return style;
    }

    /**
     * An abstraction over the POI API providing convenience methods and more of a streaming interface so that the
     * caller can avoid having to constantly manually advance to the next cell.
     * <p>
     * This class can be extended to provide further convenience methods specific to a particular type of spreadsheet
     * being written.
     */
    public static class SpreadSheetWriter {
        private Workbook workbook;
        private CellStyle preambleStyle;
        private CellStyle hyperlinkStyle;
        private Row currentRow;
        private Cell currentCell;
        private int rowNum;
        private int cellNum;

        public Sheet getCurrentSheet() {
            return currentSheet;
        }

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

        /**
         * Sets the height of the current spreadsheet row. Height is specified in "twips" or 1/20th of a point.
         *
         * @param height the height
         * @see Row#setHeight(short)
         */
        public void setRowHeight(short height) {
            currentCell.getRow().setHeight(height);
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

        public void writeCell(BigInteger value) {
            nextCell();
            if (value != null) {
                currentCell.setCellValue(value.doubleValue());
            }
        }

        public void writeCell(Double value, CellStyle cellStyle) {
            nextCell();
            if (value != null) {
                currentCell.setCellValue(value);
            }
            currentCell.setCellStyle(cellStyle);
        }

        public void writeCell(Date value, CellStyle cellStyle) {
            nextCell();
            if (value != null) {
                currentCell.setCellValue(value);
            }
            currentCell.setCellStyle(cellStyle);
        }

        public void writeCellLink(String linkText, String url) {
            nextCell();
            currentCell.setCellValue(linkText);
            Hyperlink link = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL);
            link.setAddress(url);
            currentCell.setHyperlink(link);
            currentCell.setCellStyle(hyperlinkStyle);
        }

        public void setRowStyle(CellStyle style) {
            // Using currentRow.setRowStyle() sets the style for all cells, including unused ones.
            // Instead, only set the style for the cells that have content.
            Iterator<Cell> cells = currentRow.cellIterator();
            while (cells.hasNext()) {
                cells.next().setCellStyle(style);
            }
        }

        public Workbook getWorkbook() {
            return workbook;
        }

        public void setWorkbook(Workbook workbook) {
            this.workbook = workbook;
            preambleStyle = buildPreambleStyle(workbook);
            hyperlinkStyle = buildHyperlinkStyle(workbook);
        }

        protected CellStyle buildHeaderStyle(IndexedColors indexedColors) {
            CellStyle style = workbook.createCellStyle();
            style.setFillForegroundColor(indexedColors.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setAlignment(HorizontalAlignment.CENTER);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            style.setFont(headerFont);
            return style;
        }

        /**
         * Builds a cell style for a header with text wrapping.
         */
        protected CellStyle buildWrappedHeaderStyle() {
            CellStyle style = buildHeaderStyle(IndexedColors.WHITE);
            style.setWrapText(true);
            return style;
        }
    }
}
