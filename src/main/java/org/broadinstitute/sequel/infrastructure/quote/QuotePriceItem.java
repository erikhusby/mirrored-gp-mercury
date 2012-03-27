package org.broadinstitute.sequel.infrastructure.quote;

/**
 * Simple tuple of a {@link Quote} and a
 * {@link PriceItem} that will be billed to 
 * the quote.
 */
public class QuotePriceItem {
    
    public QuotePriceItem(Quote quote,
                          PriceItem priceItem) {
        throw new RuntimeException("I haven't been implemented");
    }
    
    public Quote getQuote() {
        throw new RuntimeException("I haven't been implemented");
    }
    
    public PriceItem getPriceItem() {
        throw new RuntimeException("I haven't been implemented");
    }

    // todo equals and hashcode
}
