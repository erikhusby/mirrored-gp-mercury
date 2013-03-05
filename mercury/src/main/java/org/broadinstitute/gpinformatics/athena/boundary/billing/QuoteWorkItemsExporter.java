package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.util.AbstractSpreadsheetExporter;
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
public class QuoteWorkItemsExporter extends AbstractSpreadsheetExporter {

    private final List<QuoteImportItem> quoteItems;
    private final BillingSession billingSession;

    private static final String[] FIXED_HEADERS = { "Quote", "Platform", "Category", "Price Item", "Quantity", "Billed Date", "Billing Message"};

    public QuoteWorkItemsExporter(BillingSession billingSession, List<QuoteImportItem> quoteItems) {
        this.quoteItems = quoteItems;
        this.billingSession = billingSession;
    }

    /**
     * Write out the spreadsheet contents to a stream.  The output is in native excel format.
     *
     * @param out the stream to write to
     * @param bspUserList The user cache for getting full names from the id
     * @throws java.io.IOException if the stream can't be written to
     */
    public void writeToStream(OutputStream out, BSPUserList bspUserList) throws IOException {

        getWriter().createSheet(billingSession.getBusinessKey());

        BspUser user = bspUserList.getById(billingSession.getCreatedBy());

        String username =
                user == null ? "Unknown user: " + billingSession.getCreatedBy() : user.getFirstName() + " " + user.getLastName();

        // Write preamble.
        String preambleText = "Billing Session: " + billingSession.getBusinessKey() +
                              ", Created By: " + username +
                              ", Created Date: " + billingSession.getCreatedDate() +
                              ", Billed Date: " + billingSession.getBilledDate();

        getWriter().writePreamble(preambleText);
        getWriter().nextRow();

        // Write headers.
        getWriter().nextRow();
        for (String header : FIXED_HEADERS) {
            getWriter().writeCell(header, getFixedHeaderStyle());
        }

        for (QuoteImportItem item : quoteItems) {
            getWriter().nextRow();
            getWriter().writeCell(item.getQuoteId());
            getWriter().writeCell(item.getPriceItem().getPlatform());
            getWriter().writeCell(item.getPriceItem().getCategory());
            getWriter().writeCell(item.getPriceItem().getName());
            getWriter().writeCell(item.getQuantity());
            getWriter().writeCell((item.getWorkCompleteDate() == null) ? "" : item.getWorkCompleteDate().toString());
            getWriter().writeCell(item.getBillingMessage());
        }

        getWorkbook().write(out);
    }
}
