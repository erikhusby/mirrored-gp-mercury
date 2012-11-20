package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;

import java.util.*;

/**
 * This is the information needed to import a quantity of some price item on a quote
 */
public class QuoteImportInfo {
    /**
     * What the heck is this complicated structure? It is used to take in ledger items and bucket them
     * in a way that makes getQuoteImportItems easy later. The buckets (the map keys) are:
     *
     * Quote Id (the string that is the main key)
     * Price Item (all the info to separate a call to the quote server by primary price or add on)
     * Bill Date (this separates the counts by billing period. The get period will do the logic of placing into an appropriate chunk)
     *
     * All this maps to:
     *
     *      Billing Ledger - We keep the whole ledger so we can place any errors on each item AND it has the count
     */
    private final Map<String, Map<PriceItem, Map<Date, List<BillingLedger>>>> quantitiesByQuotePriceItem =
            new HashMap<String, Map<PriceItem, Map<Date, List<BillingLedger>>>>();

    /**
     * Take the ledger item and bucket it into the nasty structure we use here.
     *
     * @param ledger The single, ledger item
     */
    public void addQuantity(BillingLedger ledger) {
        String quoteId = ledger.getProductOrderSample().getProductOrder().getQuoteId();
        PriceItem priceItem = ledger.getPriceItem();
        Double quantity = ledger.getQuantity();

        // If we have not seen the quote yet, create the map entry for it
        if (!quantitiesByQuotePriceItem.containsKey(quoteId)) {
            quantitiesByQuotePriceItem.put(quoteId, new HashMap<PriceItem, Map<Date, List<BillingLedger>>> ());
        }

        // If the price item has not been added yet, add the quantity, otherwise, add the quantity to what was there
        if (!quantitiesByQuotePriceItem.get(quoteId).containsKey(priceItem)) {
            quantitiesByQuotePriceItem.get(quoteId).put(priceItem, new HashMap<Date, List<BillingLedger>> ());
        }

        // Get the date bucket for this price item
        Date bucketDate = getBucketDate(ledger.getDateWorkCompleted());
        if (!quantitiesByQuotePriceItem.get(quoteId).get(priceItem).containsKey(bucketDate)) {
            quantitiesByQuotePriceItem.get(quoteId).get(priceItem).put(bucketDate, new ArrayList<BillingLedger> ());
        }

        // Add this ledger item
        quantitiesByQuotePriceItem.get(quoteId).get(priceItem).get(bucketDate).add(ledger);
    }

    private Date getBucketDate(Date billedDate) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(billedDate);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        Calendar endOfPeriod = Calendar.getInstance();
        endOfPeriod.setTime(billedDate);
        if (day <= 15) {
            endOfPeriod.set(Calendar.DAY_OF_MONTH, 15);
        } else {
            endOfPeriod.set(Calendar.DAY_OF_MONTH, endOfPeriod.getActualMaximum(Calendar.DAY_OF_MONTH));
        }

        // set to end of day
        endOfPeriod.set(Calendar.HOUR_OF_DAY, 23);
        endOfPeriod.set(Calendar.MINUTE, 59);
        endOfPeriod.set(Calendar.SECOND, 59);
        endOfPeriod.set(Calendar.MILLISECOND, 999);

        return endOfPeriod.getTime();
    }

    public List<QuoteImportItem> getQuoteImportItems() {
        List<QuoteImportItem> quoteItems = new ArrayList<QuoteImportItem> ();

        for (String quoteId : quantitiesByQuotePriceItem.keySet()) {
            Map<PriceItem, Map<Date, List<BillingLedger>>> quotePriceItems = quantitiesByQuotePriceItem.get(quoteId);
            for (PriceItem priceItem : quotePriceItems.keySet()) {
                for (Date bucketDate : quotePriceItems.get(priceItem).keySet()) {
                    List<BillingLedger> ledgerItems = quotePriceItems.get(priceItem).get(bucketDate);
                    quoteItems.add(
                        new QuoteImportItem(quoteId, priceItem, ledgerItems, bucketDate));
                }
            }
        }

        return quoteItems;
    }
}
