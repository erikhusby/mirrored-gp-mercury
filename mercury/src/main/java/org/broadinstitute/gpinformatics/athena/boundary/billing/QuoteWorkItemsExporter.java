package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * This class creates a spreadsheet version of a product order's sample billing status, also called the sample
 * billing ledger.  The exported spreadsheet will later be imported after the user has updated the sample
 * billing information.
 *
 * A future version of this class will probably support both export and import, since there will be some common
 * code & data structures.
 */
public class QuoteWorkItemsExporter  {

    private final List<QuoteImportItem> quoteItems;
    private final BillingSession billingSession;

    private final Workbook workbook;
    private final Sheet sheet;
    private final CellStyle headerStyle;
    private final CellStyle preambleStyle;

    private static final String[] FIXED_HEADERS = { "Quote", "Platform", "Category", "Price Item", "Quantity", "Billed Date", "Billing Message"};

    protected CellStyle buildHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
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

    public QuoteWorkItemsExporter(BillingSession billingSession, List<QuoteImportItem> quoteItems) {
        this.quoteItems = quoteItems;
        this.billingSession = billingSession;

        // SXSSFWorkbook is used to support very large spreadsheets.  SXSSF writes 100 rows at a time to a
        // temporary file, which is then copied into the output stream when all spreadsheet data has been written.
        workbook = new SXSSFWorkbook();
        sheet = workbook.createSheet();
        headerStyle = buildHeaderStyle(workbook);
        preambleStyle = buildPreambleStyle(workbook);
    }

    private class Writer {
        Row currentRow;
        Cell currentCell;
        int rowNum;
        int cellNum;

        private void nextRow() {
            currentRow = sheet.createRow(rowNum++);
            cellNum = 0;
        }

        private void nextCell() {
            currentCell = currentRow.createCell(cellNum++);
        }

        void writePreamble(String preamble) {
            nextRow();
            writeCell(preamble, preambleStyle);
        }

        void writeCell(String value, CellStyle style) {
            nextCell();
            currentCell.setCellValue(value);
            currentCell.setCellStyle(style);
        }

        void writeCell(String value) {
            nextCell();
            currentCell.setCellValue(value);
        }

        void writeCell(double value) {
            nextCell();
            currentCell.setCellValue(value);
        }
    }

    /**
     * Write out the spreadsheet contents to a stream.  The output is in native excel format.
     *
     * @param out the stream to write to
     * @param bspUserList The user cache for getting full names from the id
     * @throws java.io.IOException if the stream can't be written to
     */
    public void writeToStream(OutputStream out, BSPUserList bspUserList) throws IOException {
        Writer writer = new Writer();

        BspUser user = bspUserList.getById(billingSession.getCreatedBy());

        // Write preamble.
        String preambleText = "Billing Session: " + billingSession.getBusinessKey() +
                              ", Created By: " + user.getFirstName() + " " + user.getLastName() +
                              ", Create Date: " + billingSession.getCreatedDate() +
                              ", Billed Date: " + billingSession.getBilledDate();

        writer.writePreamble(preambleText);
        writer.nextRow();

        // Write headers.
        writer.nextRow();
        for (String header : FIXED_HEADERS) {
            writer.writeCell(header, headerStyle);
        }

        for (QuoteImportItem item : quoteItems) {
            writer.nextRow();
            writer.writeCell(item.getQuoteId());
            writer.writeCell(item.getPriceItem().getPlatform());
            writer.writeCell(item.getPriceItem().getCategory());
            writer.writeCell(item.getPriceItem().getName());
            writer.writeCell(item.getQuantity());
            writer.writeCell((item.getBilledDate() == null) ? "" : item.getBilledDate().toString());
            writer.writeCell(item.getBillingMessage());
        }

        workbook.write(out);
    }
}
