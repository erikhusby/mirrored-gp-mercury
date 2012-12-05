package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;

import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

/**
 * A flattened structure of information needed to import an item into the quote server
 */
public class QuoteImportItem {
    private final String quoteId;
    private final PriceItem priceItem;
    private final Date billToDate;
    private final List<BillingLedger> ledgerItems;
    private Date startRange;
    private Date endRange;

    public QuoteImportItem(
        String quoteId, PriceItem priceItem, List<BillingLedger> ledgerItems, Date billToDate) {

        this.quoteId = quoteId;
        this.priceItem = priceItem;
        this.ledgerItems = ledgerItems;
        this.billToDate = billToDate;

        for (BillingLedger ledger : ledgerItems) {
            updateDateRange(ledger.getWorkCompleteDate());
        }
    }

    private void updateDateRange(Date completedDate) {
        if (startRange == null) {
            startRange = completedDate;
            endRange = completedDate;
            return;
        }

        if (completedDate.before(startRange)) {
            startRange = completedDate;
        }

        if (completedDate.after(endRange)) {
            endRange = completedDate;
        }
    }

    public String getQuoteId() {
        return quoteId;
    }

    public PriceItem getPriceItem() {
        return priceItem;
    }

    public double getQuantity() {
        double quantity = 0;
        for (BillingLedger ledgerItem : ledgerItems) {
            quantity += ledgerItem.getQuantity();
        }
        return quantity;
    }

    public Date getWorkCompleteDate() {
        return billToDate;
    }

    public String getBillingMessage() {
        // Since the quote message will apply to all items, just pull the message off the first item
        return ledgerItems.get(0).getBillingMessage();
    }

    public void setBillingMessages(String billedMessage) {
        for (BillingLedger ledgerItem : ledgerItems) {
            ledgerItem.setBillingMessage(billedMessage);
        }
    }

    public Date getStartRange() {
        return startRange;
    }

    public Date getEndRange() {
        return endRange;
    }

    public String getNumSamples() {
        return MessageFormat.format("{0} Sample{0, choice, 0#s|1#|1<s}", ledgerItems.size());
    }
}
