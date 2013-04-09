package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;

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
    private final List<LedgerEntry> ledgerItems;
    private Date startRange;
    private Date endRange;

    public QuoteImportItem(
        String quoteId, PriceItem priceItem, List<LedgerEntry> ledgerItems, Date billToDate) {

        this.quoteId = quoteId;
        this.priceItem = priceItem;
        this.ledgerItems = ledgerItems;
        this.billToDate = billToDate;

        for (LedgerEntry ledger : ledgerItems) {
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
        for (LedgerEntry ledgerItem : ledgerItems) {
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
        for (LedgerEntry ledgerItem : ledgerItems) {
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

    /**
     * This method should be invoked upon successful billing to update ledger entries with the quote to which they were
     * billed.
     *
     * @param itemIsReplacing The item that is replacing the primary price item.
     * @param billingMessage The message to be assigned to all entries.
     */
    public void updateQuoteIntoLedgerEntries(
        org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem itemIsReplacing,
        String billingMessage) {

        LedgerEntry.PriceItemType priceItemType = getPriceItemType(itemIsReplacing);

        for (LedgerEntry ledgerEntry : ledgerItems) {
            ledgerEntry.setQuoteId(quoteId);
            ledgerEntry.setPriceItemType(priceItemType);
            ledgerEntry.setBillingMessage(billingMessage);
        }
    }

    /**
     * @return There should always be ledger entries and if not, this failing will be fine.
     */
    public Product getProduct() {
        return ledgerItems.get(0).getProductOrderSample().getProductOrder().getProduct();
    }

    /**
     * Calculate if this item's price item is an optional price item on this product.
     *
     * @param priceListCache The cache of the price list.
     *
     * @return null if this is not a replacement item or the primary price item if it is one.
     */
    public PriceItem calculateIsReplacing(PriceListCache priceListCache) {
        Product product = getProduct();

        // If this is optional, then return the primary as the 'is replacing.' This is comparing the quote price item
        // to the values on the product's price item, so do the item by item compare
        for (org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem optional : product.getReplacementPriceItems(priceListCache)) {
            if (optional.isMercuryPriceItemEqual(priceItem)) {
                return product.getPrimaryPriceItem();
            }
        }

        return null;
    }

    public LedgerEntry.PriceItemType getPriceItemType(org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem itemIsReplacing) {
        LedgerEntry.PriceItemType type;
        if (itemIsReplacing != null) {
            type = LedgerEntry.PriceItemType.ReplacementPriceItem;
        } else if (getProduct().getPrimaryPriceItem().getName().equals(getPriceItem().getName())) {
            type = LedgerEntry.PriceItemType.PrimaryPriceItem;
        } else {
            // If it is not the primary or replacement right now, it has to be considered add on
            type = LedgerEntry.PriceItemType.AddOnPriceItem;
        }

        return type;
    }
}
