package org.broadinstitute.sequel;


import java.util.Collection;

public class InvoiceLineItemImpl implements InvoiceLineItem {

    public InvoiceLineItemImpl(StartingSample aliquot, Collection<Quote> possibleQuotes) {

    }
    
    @Override
    public Collection<Quote> getPossibleQuotes() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void setQuote(Quote q, Person user) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Quote getQuote() {
        throw new RuntimeException("I haven't been written yet.");
    }
}
