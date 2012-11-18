package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;

/**
 * A flattened structure of information needed to import an item into the quote server
 */
public class QuoteImportItem {
    private final String quoteId;
    private final PriceItem priceItem;
    private final Double quantity;

    public QuoteImportItem(String quoteId, PriceItem priceItem, Double quantity) {
        this.quoteId = quoteId;
        this.priceItem = priceItem;
        this.quantity = quantity;
    }

    public String getQuoteId() {
        return quoteId;
    }

    public PriceItem getPriceItem() {
        return priceItem;
    }

    public Double getQuantity() {
        return quantity;
    }
}
