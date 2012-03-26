package org.broadinstitute.sequel.entity.billing;

/**
 * Quote entity, which is just a alphanumeric id
 * and a wrapper around a quote DTO for more
 * details.
 */
public class Quote {
    
    private final org.broadinstitute.sequel.infrastructure.quote.Quote quoteDTO;
    
    private final String alphanumericId;

    /**
     * @param alphanumericId the alphanumeric id of the quote,
     *                       to be persisted in sequel
     * @param quoteDTO everything else about the quote,
     *                 fetched from the quote server
     */
    public Quote(String alphanumericId,
                 org.broadinstitute.sequel.infrastructure.quote.Quote quoteDTO) {
        this.quoteDTO = quoteDTO;
        this.alphanumericId = alphanumericId;
    }
    
    public org.broadinstitute.sequel.infrastructure.quote.Quote getQuote() {
        return quoteDTO;
    }
}
