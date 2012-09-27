package org.broadinstitute.gpinformatics.infrastructure.quote;

public interface Billable {

    public void doBilling(QuoteService quoteService);
}
