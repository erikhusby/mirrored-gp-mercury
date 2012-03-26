package org.broadinstitute.sequel.entity.billing;

public class Quote {
    
    private final org.broadinstitute.sequel.infrastructure.quote.Quote quoteDTO;
    
    private final String alphanumericId;
    
    public Quote(String alphanumericId,
                 org.broadinstitute.sequel.infrastructure.quote.Quote quoteDTO) {
        this.quoteDTO = quoteDTO;
        this.alphanumericId = alphanumericId;
    }
    
    public org.broadinstitute.sequel.infrastructure.quote.Quote getQuote() {
        return quoteDTO;
    }
}
