package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A flattened structure of information needed to import an item into the quote server.
 */
public class QuoteImportItem {
    private final String quoteId;
    private final PriceItem priceItem;
    private String quotePriceType;
    private final Date billToDate;
    private final List<LedgerEntry> ledgerItems;
    private Date startRange;
    private Date endRange;

    public QuoteImportItem(
            String quoteId, PriceItem priceItem, String quotePriceType, List<LedgerEntry> ledgerItems, Date billToDate) {

        this.quoteId = quoteId;
        this.priceItem = priceItem;
        this.quotePriceType = quotePriceType;
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
        // Since the quote message will apply to all items, just pull the message off the first item.
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
     * billed and the work item.
     *
     * @param itemIsReplacing The item that is replacing the primary price item.
     * @param billingMessage The message to be assigned to all entries.
     * @param quoteServerWorkItem the id of the transaction in the quote server
     */
    public void updateLedgerEntries(QuotePriceItem itemIsReplacing, String billingMessage,String quoteServerWorkItem) {

        LedgerEntry.PriceItemType priceItemType = getPriceItemType(itemIsReplacing);

        for (LedgerEntry ledgerEntry : ledgerItems) {
            ledgerEntry.setQuoteId(quoteId);
            ledgerEntry.setPriceItemType(priceItemType);
            ledgerEntry.setBillingMessage(billingMessage);
            ledgerEntry.setWorkItem(quoteServerWorkItem);
        }
    }

    /**
     * @return There should always be ledger entries and if not, it will throw an exception, which should be OK. This
     * just returns the first items sample because all items are grouped at a fine level by price item which means the
     * same product because price items are product based.
     */
    public Product getProduct() {
        return ledgerItems.get(0).getProductOrderSample().getProductOrder().getProduct();
    }

    public Collection<LedgerEntry> getLedgerItems() {
        return ledgerItems;
    }
    /**
     * Calculate if this item's price item is a replacement price item on this product. It returns a quote price item
     * object that is the primary.
     *
     * @param priceListCache The cache of the price list.
     *
     * @return null if this is not a replacement item or the primary price item if it is one.
     */
    public QuotePriceItem getPrimaryForReplacement(PriceListCache priceListCache) {
        PriceItem primaryPriceItem = getProduct().getPrimaryPriceItem();

        // If this is optional, then return the primary as the 'is replacing.' This is comparing the quote price item
        // to the values on the product's price item, so do the item by item compare.
        for (QuotePriceItem optional : priceListCache.getReplacementPriceItems(primaryPriceItem)) {
            if (optional.isMercuryPriceItemEqual(priceItem)) {
                return QuotePriceItem.convertMercuryPriceItem(primaryPriceItem);
            }
        }

        return null;
    }

    public LedgerEntry.PriceItemType getPriceItemType(QuotePriceItem itemIsReplacing) {
        LedgerEntry.PriceItemType type;
        if (itemIsReplacing != null) {
            type = LedgerEntry.PriceItemType.REPLACEMENT_PRICE_ITEM;
        } else if (getProduct().getPrimaryPriceItem().getName().equals(getPriceItem().getName())) {
            type = LedgerEntry.PriceItemType.PRIMARY_PRICE_ITEM;
        } else {
            // If it is not the primary or replacement right now, it has to be considered add on.
            type = LedgerEntry.PriceItemType.ADD_ON_PRICE_ITEM;
        }

        return type;
    }

    public String getQuotePriceType() {
        return quotePriceType;
    }

    public void setQuotePriceType(String quotePriceType) {
        this.quotePriceType = quotePriceType;
    }

    /**
     * @return a list of keys of all PDOs that are affected by this collection of ledger items.
     */
    public Collection<String> getOrderKeys() {
        Set<String> keys = new HashSet<>();
        for (LedgerEntry entry : ledgerItems) {
            keys.add(entry.getProductOrderSample().getProductOrder().getJiraTicketKey());
        }
        return keys;
    }
}
