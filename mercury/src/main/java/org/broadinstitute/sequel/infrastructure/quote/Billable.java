package org.broadinstitute.sequel.infrastructure.quote;

public interface Billable {

    public void doBilling(QuoteService quoteService);
}
