package org.broadinstitute.sequel;

import java.util.Collection;

/**
 * A line item is some individually billable
 * thing.  For example:
 *
 * 1/93rd of a flowcell lane
 *
 * 1/384th of an adaptor ligation plate event
 */
public interface InvoiceLineItem {

    /**
     * The list of possible quotes to use
     * for this line item.
     *
     * We expect this list to be initialized
     * by {@link InvoiceGenerator#generateInvoice(Priceable)}  the invoice generator}
     * @return
     */
    public Collection<Quote> getPossibleQuotes();

    /**
     * The exact quote to use for this line item.
     * A PM chooses which exact quote to use,
     * which might come from {@link #getPossibleQuotes()
     * the list of possible quotes} or might not.
     * @param q
     * @param user
     */
    public void setQuote(Quote q,Person user);

    /**
     * Gets the single quote to be billed
     * for this line item.
     */
    public Quote getQuote();

}
