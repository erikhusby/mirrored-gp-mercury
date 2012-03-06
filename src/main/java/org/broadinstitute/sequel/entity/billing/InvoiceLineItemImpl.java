package org.broadinstitute.sequel.entity.billing;


import org.broadinstitute.sequel.control.quote.Quote;
import org.broadinstitute.sequel.entity.person.Person;
import org.broadinstitute.sequel.entity.sample.StartingSample;

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
