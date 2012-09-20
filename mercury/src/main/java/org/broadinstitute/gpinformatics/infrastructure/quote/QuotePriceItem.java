package org.broadinstitute.gpinformatics.infrastructure.quote;

/**
 * Simple tuple of a {@link Quote} and a
 * {@link PriceItem} that will be billed to 
 * the quote.
 */
public class QuotePriceItem {
    
    private final Quote quote;

    private final PriceItem priceItem;
    
    public QuotePriceItem(Quote quote,
                          PriceItem priceItem) {
        if (quote == null) {
            throw new NullPointerException("quote cannot be null");
        }
        if (priceItem == null) {
            throw new NullPointerException("priceItem cannot be null");
        }
        this.quote = quote;
        this.priceItem = priceItem;
    }
    
    public Quote getQuote() {
        return quote;
    }
    
    public PriceItem getPriceItem() {
        return priceItem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QuotePriceItem that = (QuotePriceItem) o;

        if (!priceItem.equals(that.priceItem)) return false;
        if (!quote.equals(that.quote)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = quote.hashCode();
        result = 31 * result + priceItem.hashCode();
        return result;
    }
}
