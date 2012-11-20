package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;

import java.util.Date;
import java.util.List;

/**
 * A flattened structure of information needed to import an item into the quote server
 */
public class QuoteImportItem {
    private final String quoteId;
    private final PriceItem priceItem;
    private final List<BillingLedger> ledgerItems;

    public QuoteImportItem(String quoteId, PriceItem priceItem, List<BillingLedger> ledgerItems) {
        this.quoteId = quoteId;
        this.priceItem = priceItem;
        this.ledgerItems = ledgerItems;
    }

    public String getQuoteId() {
        return quoteId;
    }

    public PriceItem getPriceItem() {
        return priceItem;
    }

    public Double getQuantity() {
        double quantity = 0d;
        for (BillingLedger ledgerItem : ledgerItems) {
            quantity += ledgerItem.getQuantity();
        }
        return quantity;
    }

    public Date getWorkCompleteDate() {
        // Since the quote message will apply to all items, just pull the date off the first item
        return ledgerItems.get(0).getWorkCompleteDate();
    }

    public String getBillingMessage() {
        // Since the quote message will apply to all items, just pull the message off the first item
        return ledgerItems.get(0).getBillingMessage();
    }

    public void setupBilledInfo(String billedMessage) {
        Date currentDate = new Date();

        for (BillingLedger ledgerItem : ledgerItems) {
            ledgerItem.setWorkCompleteDate(currentDate);
            ledgerItem.setBillingMessage(billedMessage);
        }
    }

    public void setupBillError(String errorMessage) {
        for (BillingLedger ledgerItem : ledgerItems) {
            ledgerItem.setBillingMessage(errorMessage);
        }
    }
}
