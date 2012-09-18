package org.broadinstitute.gpinformatics.mercury.infrastructure.quote;

public interface Billable {

    public void doBilling(QuoteService quoteService);
}
