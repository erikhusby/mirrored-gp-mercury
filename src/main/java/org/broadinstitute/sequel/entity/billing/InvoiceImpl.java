package org.broadinstitute.sequel.entity.billing;


import java.util.Collection;

public class InvoiceImpl implements Invoice {

    public InvoiceImpl(Priceable priceable,String invoiceName) {

    }
    
    @Override
    public String getInvoiceName() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<InvoiceLineItem> getInvoiceLineItems() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void remove(InvoiceLineItem lineItem) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void add(InvoiceLineItem lineItem) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void send() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Priceable getPriceable() {
        throw new RuntimeException("I haven't been written yet.");
    }
}
