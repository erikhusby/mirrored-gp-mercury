package org.broadinstitute.gpinformatics.athena.boundary.util;

import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Wrapper for all exporters so that the writer and exporter can share some members
 */
public abstract class AbstractSpreadsheetExporter {

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private final Workbook workbook;

    private final CellStyle fixedHeaderStyle;
    private final CellStyle priceItemProductHeaderStyle;
    private final CellStyle billedAmountsHeaderStyle;
    private final CellStyle preambleStyle;
    private final CellStyle previouslyBilledStyle;
    private final CellStyle errorMessageStyle;

    private final SpreadSheetWriter writer = new SpreadSheetWriter();

    public AbstractSpreadsheetExporter() {
        // SXSSFWorkbook is used to support very large spreadsheets.  SXSSF writes 100 rows at a time to a
        // temporary file, which is then copied into the output stream when all spreadsheet data has been written.
        workbook = new SXSSFWorkbook();
        fixedHeaderStyle = buildFixedHeaderStyle(workbook);
        priceItemProductHeaderStyle = buildPriceItemProductHeaderStyle(workbook);
        billedAmountsHeaderStyle = buildBilledAmountsHeaderStyle(workbook);
        preambleStyle = buildPreambleStyle(workbook);
        previouslyBilledStyle = buildPreviouslyBilledStyle(workbook);
        errorMessageStyle = buildErrorMessageStyle(workbook);
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

    protected CellStyle getErrorMessageStyle() {
        return errorMessageStyle;
    }

    protected CellStyle getBilledAmountsHeaderStyle() {
        return billedAmountsHeaderStyle;
    }


    protected CellStyle getPreviouslyBilledStyle() {
        return previouslyBilledStyle;
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


    protected CellStyle buildFixedHeaderStyle(Workbook wb) {
        return buildHeaderStyle(wb, IndexedColors.LIGHT_CORNFLOWER_BLUE);
    }


    protected CellStyle buildPriceItemProductHeaderStyle(Workbook wb) {
        return buildHeaderStyle(wb, IndexedColors.GREY_25_PERCENT);
    }


    protected CellStyle buildBilledAmountsHeaderStyle(Workbook wb) {
        return buildHeaderStyle(wb, IndexedColors.LIGHT_YELLOW);
    }


    protected CellStyle buildPreambleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font headerFont = wb.createFont();
        headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
        style.setFont(headerFont);
        return style;
    }

    protected CellStyle buildErrorMessageStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.RED.getIndex());
        style.setFillPattern(CellStyle.THICK_FORWARD_DIAG);
        style.setAlignment(CellStyle.ALIGN_LEFT);
        style.setWrapText(true);
        Font headerFont = wb.createFont();
        headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
        headerFont.setColor(IndexedColors.RED.getIndex());
        style.setFont(headerFont);
        return style;

    }

    protected CellStyle buildPreviouslyBilledStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setAlignment(CellStyle.ALIGN_CENTER);
        Font headerFont = wb.createFont();
        headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
        headerFont.setColor(IndexedColors.RED.getIndex());
        style.setFont(headerFont);
        return style;

    }

    /**
     * This handles the details of sending a stream of data back through the faces context as a download.
     *
     * @param inputStream The stream of data
     * @param filename The name of the file to be sent to the browser
     *
     * @throws IOException Any errors
     */
    public static void copyForDownload(InputStream inputStream, String filename) throws IOException {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        OutputStream finalOutputStream = AbstractSpreadsheetExporter.beginSpreadsheetDownload(facesContext, filename);
        IOUtils.copy(inputStream, finalOutputStream);

        // Since this is a transfer, then the response is done and nothing needs to be displayed
        facesContext.responseComplete();
    }

    /**
     * Utility function for setting up the excel spreadsheet on the response
     *
     * @param fc The faces context to grab the response from
     * @param filename The name of the file to use
     *
     * @return The output stream to grab
     * @throws IOException Any errors
     */
    private static OutputStream beginSpreadsheetDownload(FacesContext fc, String filename) throws IOException {

        HttpServletResponse response = (HttpServletResponse) fc.getExternalContext().getResponse();

        response.reset();
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        return response.getOutputStream();
    }

    public class SpreadSheetWriter {
        private Row currentRow;
        private Cell currentCell;
        private int rowNum;
        private int cellNum;
        private Sheet currentSheet;

        public SpreadSheetWriter() {
        }

        public void setCurrentSheet(Sheet currentSheet) {
            this.currentSheet = currentSheet;
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


        public void writeCell(Date value) {
            nextCell();

            if (value != null) {
                currentCell.setCellValue(value);
            }
        }
    }
}
