package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the information needed to import a quantity of some price item on a quote
 */
public class QuoteImportInfo {
    private final Map<String, Map<PriceItem, Double>> quantitiesByQuotePriceItem =
            new HashMap<String, Map<PriceItem, Double>>();

    public void addQuantity(String quoteId, PriceItem priceItem, Double quantity) {

        // If we have not seen the quote yet, create the map entry for it
        if (!quantitiesByQuotePriceItem.containsKey(quoteId)) {
            quantitiesByQuotePriceItem.put(quoteId, new HashMap<PriceItem, Double>());
        }

        // If the price item has not been added yet, add the quantity, otherwise, add the quantity to what was there
        if (!quantitiesByQuotePriceItem.get(quoteId).containsKey(priceItem)) {
            quantitiesByQuotePriceItem.get(quoteId).put(priceItem, quantity);
        } else {
            Double original = quantitiesByQuotePriceItem.get(quoteId).get(priceItem);
            quantitiesByQuotePriceItem.get(quoteId).put(priceItem, quantity + original);
        }
    }

    public List<QuoteImportItem> getQuoteImportItems() {
        List<QuoteImportItem> quoteItems = new ArrayList<QuoteImportItem> ();

        for (String quoteId : quantitiesByQuotePriceItem.keySet()) {
            Map<PriceItem, Double> quotePriceItems = quantitiesByQuotePriceItem.get(quoteId);
            for (PriceItem priceItem : quotePriceItems.keySet()) {
                Double quantity = quotePriceItems.get(priceItem);

                quoteItems.add(new QuoteImportItem(quoteId, priceItem, quantity));
            }
        }

        return quoteItems;
    }
}
