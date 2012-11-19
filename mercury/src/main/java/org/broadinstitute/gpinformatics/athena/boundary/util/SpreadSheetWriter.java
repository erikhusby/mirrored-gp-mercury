package org.broadinstitute.gpinformatics.athena.boundary.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * This is a utility class for writing out cells in a spreadsheet in a general way
 */
public class SpreadSheetWriter {
    private Row currentRow;
    private Cell currentCell;
    private int rowNum;
    private int cellNum;

    private Sheet sheet;
    CellStyle preambleStyle;

    public SpreadSheetWriter(Sheet sheet, CellStyle preambleStyle) {
        this.sheet = sheet;
        this.preambleStyle = preambleStyle;
    }

    public void nextRow() {
        currentRow = sheet.createRow(rowNum++);
        cellNum = 0;
    }

    public void nextCell() {
        currentCell = currentRow.createCell(cellNum++);
    }

    public void writePreamble(String preamble) {
        nextRow();
        writeCell(preamble, preambleStyle);
    }

    public void writeCell(String value, CellStyle style) {
        nextCell();
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
}
