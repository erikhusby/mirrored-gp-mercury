package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.broadinstitute.gpinformatics.athena.boundary.util.SpreadSheetWriter;
import org.broadinstitute.gpinformatics.athena.entity.orders.BillableItem;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.*;

/**
 * This class creates a spreadsheet version of a product order's sample billing status, also called the sample
 * billing ledger.  The exported spreadsheet will later be imported after the user has updated the sample
 * billing information.
 *
 * A future version of this class will probably support both export and import, since there will be some common
 * code & data structures.
 */
public class SampleLedgerExporter {

    private final ProductOrder productOrder;

    private final List<PriceItem> priceItems;

    private final Workbook workbook;
    private final Sheet sheet;
    private final CellStyle headerStyle;
    private final CellStyle preambleStyle;

    private static final String[] FIXED_HEADERS = { "Sample ID", "Billing Status"};

    /** Count shown when no billing has occurred. */
    private static final String NO_BILL_COUNT = "0";

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

    public SampleLedgerExporter(ProductOrder productOrder) {
        this.productOrder = productOrder;
        // SXSSFWorkbook is used to support very large spreadsheets.  SXSSF writes 100 rows at a time to a
        // temporary file, which is then copied into the output stream when all spreadsheet data has been written.
        workbook = new SXSSFWorkbook();
        sheet = workbook.createSheet();
        headerStyle = buildHeaderStyle(workbook);
        preambleStyle = buildPreambleStyle(workbook);

        // Create a copy of the product's price items list in order to impose an order on it.
        priceItems = new ArrayList<PriceItem>(productOrder.getProduct().getPriceItems());
        Collections.sort(priceItems, new Comparator<PriceItem>() {
            @Override
            public int compare(PriceItem o1, PriceItem o2) {
                return new CompareToBuilder().append(o1.getPlatform(), o2.getPlatform())
                        .append(o1.getCategory(), o2.getCategory())
                        .append(o1.getName(), o2.getName()).build();
            }
        });
    }

    private static Map<PriceItem, BigDecimal> getBillCounts(ProductOrderSample sample) {
        Set<BillableItem> billableItems = sample.getBillableItems();
        if (billableItems.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<PriceItem, BigDecimal> sampleStatus = new HashMap<PriceItem, BigDecimal>();
        for (BillableItem item : billableItems) {
            sampleStatus.put(item.getPriceItem(), item.getCount());
        }
        return sampleStatus;
    }

    /**
     * Write out the spreadsheet contents to a stream.  The output is in native excel format.
     * @param out the stream to write to
     * @throws IOException if the stream can't be written to
     */
    public void writeToStream(OutputStream out) throws IOException {
        SpreadSheetWriter writer = new SpreadSheetWriter(sheet, preambleStyle);

        // Write preamble.
        String preambleText = "Order: " + productOrder.getJiraTicketKey() +
                              ", Title: " + productOrder.getTitle() +
                              ", Product: " + productOrder.getProduct().getProductName();
        writer.writePreamble(preambleText);

        // Write headers.
        writer.nextRow();
        for (String header : FIXED_HEADERS) {
            writer.writeCell(header, headerStyle);
        }
        for (PriceItem item : priceItems) {
            writer.writeCell(item.getCategory() + " - " + item.getName(), headerStyle);
        }

        // Write content.
        for (ProductOrderSample sample : productOrder.getSamples()) {
            writer.nextRow();
            writer.writeCell(sample.getSampleName());
            writer.writeCell(sample.getBillingStatus().getDisplayName());

            Map<PriceItem, BigDecimal> billCounts = getBillCounts(sample);
            for (PriceItem item : priceItems) {
                BigDecimal count = billCounts.get(item);
                if (count != null) {
                    writer.writeCell(count.doubleValue());
                } else {
                    writer.writeCell(NO_BILL_COUNT);
                }
            }
        }

        workbook.write(out);
    }
}
