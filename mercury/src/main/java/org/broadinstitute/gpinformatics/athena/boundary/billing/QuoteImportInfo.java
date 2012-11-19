package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the information needed to import a quantity of some price item on a quote
 */
public class QuoteImportInfo {
    private final Map<String, Map<PriceItem, List<BillingLedger>>> quantitiesByQuotePriceItem =
            new HashMap<String, Map<PriceItem, List<BillingLedger>>>();

    public void addQuantity(BillingLedger ledger) {
        String quoteId = ledger.getProductOrderSample().getProductOrder().getQuoteId();
        PriceItem priceItem = ledger.getPriceItem();
        Double quantity = ledger.getQuantity();

        // If we have not seen the quote yet, create the map entry for it
        if (!quantitiesByQuotePriceItem.containsKey(quoteId)) {
            quantitiesByQuotePriceItem.put(quoteId, new HashMap<PriceItem, List<BillingLedger>> ());
        }

        // If the price item has not been added yet, add the quantity, otherwise, add the quantity to what was there
        if (!quantitiesByQuotePriceItem.get(quoteId).containsKey(priceItem)) {
            quantitiesByQuotePriceItem.get(quoteId).put(priceItem, new ArrayList<BillingLedger>());
        }

        // Add this ledger item
        quantitiesByQuotePriceItem.get(quoteId).get(priceItem).add(ledger);
    }

    public List<QuoteImportItem> getQuoteImportItems() {
        List<QuoteImportItem> quoteItems = new ArrayList<QuoteImportItem> ();

        for (String quoteId : quantitiesByQuotePriceItem.keySet()) {
            Map<PriceItem, List<BillingLedger>> quotePriceItems = quantitiesByQuotePriceItem.get(quoteId);
            for (PriceItem priceItem : quotePriceItems.keySet()) {
                quoteItems.add(new QuoteImportItem(quoteId, priceItem, quotePriceItems.get(priceItem)));
            }
        }

        return quoteItems;
    }
}
