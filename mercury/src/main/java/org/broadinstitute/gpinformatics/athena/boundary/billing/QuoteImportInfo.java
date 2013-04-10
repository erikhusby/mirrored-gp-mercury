package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;

import javax.inject.Inject;
import java.util.*;

/**
 * This is the information needed to import a quantity of some price item on a quote
 */
public class QuoteImportInfo {

    @Inject
    private PriceListCache priceListCache;

    /**
     * What the heck is this complicated structure? It is used to take in ledger items and bucket them
     * in a way that makes getQuoteImportItems easy later. The buckets (the map keys) are:
     * <ul>
     * <li>Quote Id (the string that is the main key)</li>
     * <li>Price Item (all the info to separate a call to the quote server by primary price or add on)</li>
     * <li>Bill Date (this separates the counts by billing period. The get period will do the logic of placing into an appropriate chunk)</li>
     * </ul>
     * All this maps to:
     * <p/>
     *      Billing Ledger - We keep the whole ledger so we can place any errors on each item AND it has the count
     */
    private final Map<String, Map<PriceItem, Map<Date, List<LedgerEntry>>>> quantitiesByQuotePriceItem =
            new HashMap<String, Map<PriceItem, Map<Date, List<LedgerEntry>>>>();

    /**
     * Take the ledger item and bucket it into the nasty structure we use here.
     *
     * @param ledger The single, ledger item
     */
    public void addQuantity(LedgerEntry ledger) {

        // Get the appropriate quote id (the one we will bill or the one we did bill depending on state).
        String quoteId = getLedgerQuoteId(ledger);

        // The price item on the ledger entry.
        PriceItem priceItem = ledger.getPriceItem();

        // If we have not seen the quote yet, create the map entry for it.
        if (!quantitiesByQuotePriceItem.containsKey(quoteId)) {
            quantitiesByQuotePriceItem.put(quoteId, new HashMap<PriceItem, Map<Date, List<LedgerEntry>>> ());
        }

        // If the price item has not been added yet, add the quantity, otherwise, add the quantity to what was there.
        if (!quantitiesByQuotePriceItem.get(quoteId).containsKey(priceItem)) {
            quantitiesByQuotePriceItem.get(quoteId).put(priceItem, new HashMap<Date, List<LedgerEntry>> ());
        }

        // Get the date bucket for this price item.
        Date bucketDate = getBucketDate(ledger.getWorkCompleteDate());
        if (!quantitiesByQuotePriceItem.get(quoteId).get(priceItem).containsKey(bucketDate)) {
            quantitiesByQuotePriceItem.get(quoteId).get(priceItem).put(bucketDate, new ArrayList<LedgerEntry> ());
        }

        // Add this ledger item.
        quantitiesByQuotePriceItem.get(quoteId).get(priceItem).get(bucketDate).add(ledger);
    }

    /**
     * Return the quote on the ledger. If the ledger is not yet billed it won't be set, so return the quote on the
     * the product order since this is the one that will be billed.
     *
     * @param ledger The ledger entry being looked at.
     *
     * @return The quote identifier.
     */
    private static String getLedgerQuoteId(LedgerEntry ledger) {
        if (!StringUtils.isBlank(ledger.getQuoteId())) {
            return ledger.getQuoteId();
        }

        return ledger.getProductOrderSample().getProductOrder().getQuoteId();
    }

    private static Date getBucketDate(Date billedDate) {
        // Get the end of the period.
        Calendar endOfPeriod = Calendar.getInstance();
        endOfPeriod.setTime(billedDate);

        // Set to the end of the day so anything that is ever sent with time will normalize to the same bucket.
        endOfPeriod.set(Calendar.HOUR_OF_DAY, 23);
        endOfPeriod.set(Calendar.MINUTE, 59);
        endOfPeriod.set(Calendar.SECOND, 59);
        endOfPeriod.set(Calendar.MILLISECOND, 999);

        return endOfPeriod.getTime();
    }

    public List<QuoteImportItem> getQuoteImportItems() {
        List<QuoteImportItem> quoteItems = new ArrayList<QuoteImportItem> ();

        for (String quoteId : quantitiesByQuotePriceItem.keySet()) {
            Map<PriceItem, Map<Date, List<LedgerEntry>>> quotePriceItems = quantitiesByQuotePriceItem.get(quoteId);
            for (PriceItem priceItem : quotePriceItems.keySet()) {
                for (Date bucketDate : quotePriceItems.get(priceItem).keySet()) {
                    List<LedgerEntry> ledgerItems = quotePriceItems.get(priceItem).get(bucketDate);

                    List<LedgerEntry> creditLedgerItems = new ArrayList<LedgerEntry>();
                    List<LedgerEntry> debitLedgerItems = new ArrayList<LedgerEntry>();
                    List<LedgerEntry> replacementCreditLedgerItems = new ArrayList<LedgerEntry>();
                    List<LedgerEntry> replacementDebitLedgerItems = new ArrayList<LedgerEntry>();

                    // Separate the items into debits and credits so that the quote server will not cancel out items.
                    for (LedgerEntry ledger : ledgerItems) {
                        if (ledger.getQuantity() < 0) {
                            if (isReplacementPriceItem(ledger)) {
                                replacementCreditLedgerItems.add(ledger);
                            } else {
                                creditLedgerItems.add(ledger);
                            }
                        } else {
                            if (isReplacementPriceItem(ledger)) {
                                replacementDebitLedgerItems.add(ledger);
                            } else {
                                debitLedgerItems.add(ledger);
                            }
                        }
                    }

                    addQuoteItemsForLedgerItems(quoteItems, quoteId, priceItem,
                            LedgerEntry.PriceItemType.PRIMARY_PRICE_ITEM.getQuoteType(),
                            debitLedgerItems, bucketDate);

                    addQuoteItemsForLedgerItems(quoteItems, quoteId, priceItem,
                            LedgerEntry.PriceItemType.REPLACEMENT_PRICE_ITEM.getQuoteType(),
                            replacementDebitLedgerItems, bucketDate);

                    addQuoteItemsForLedgerItems(quoteItems, quoteId, priceItem,
                            LedgerEntry.PriceItemType.PRIMARY_PRICE_ITEM.getQuoteType(),
                            creditLedgerItems, bucketDate);

                    addQuoteItemsForLedgerItems(quoteItems, quoteId, priceItem,
                            LedgerEntry.PriceItemType.REPLACEMENT_PRICE_ITEM.getQuoteType(),
                            replacementCreditLedgerItems, bucketDate);
                }
            }
        }

        return quoteItems;
    }

    private void addQuoteItemsForLedgerItems(
        List<QuoteImportItem> quoteItems, String quoteId, PriceItem priceItem, String quoteType,
        List<LedgerEntry> ledgerItems, Date bucketDate) {

        if (!ledgerItems.isEmpty()) {
            QuoteImportItem newQuoteItem = new QuoteImportItem(quoteId, priceItem, quoteType, ledgerItems, bucketDate);
            quoteItems.add(newQuoteItem);
        }
    }

    /**
     * This tests whether the ledger entry is a quote item or a quote replacement item. If the session was
     * already billed (a quote is applied), then pull this right off the ledger. If this has not been billed, then
     * calculate it from the product order.
     *
     * @param ledger The ledger entry.
     * @return Is the entry a replacement or not.
     */
    private boolean isReplacementPriceItem(LedgerEntry ledger) {
        if (ledger.getQuoteId() != null) {
            return LedgerEntry.PriceItemType.REPLACEMENT_PRICE_ITEM == ledger.getPriceItemType();
        } else {
            // No quote, so calculate what it would be given the state of things now.
            Collection<org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem> quotePriceItems =
                ledger.getProductOrderSample().getProductOrder().getProduct().getReplacementPriceItems(priceListCache);

            for (org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem quotePriceItem : quotePriceItems) {
                if (ledger.getPriceItem().getName().equals(quotePriceItem.getName())) {
                    return true;
                }
            }
        }

        return false;
    }
}
