package org.broadinstitute.sequel.entity.billing;


import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.control.billing.InvoiceGenerator;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Probably for most billable events the
 * finest granularity is the aliquot.
 *
 * For a 384 well plate, one might think
 * the finest granularity is a well.  But
 * what if we pooled 800 samples from
 * different collaborators in each well?
 *
 * This invoice generator uses the sample
 * sheet to find all the aliquots in the
 * priceable, and for each aliquot, it takes
 * all possible quotes from all active projects
 * to build the set of possible quotes.
 *
 * If we haven't already noticed what a pain
 * in the ass not having 1-1 aliquot/project
 * relationships is, here's where we'll notice
 * it.  And yet we have to allow for this.
 */
public class PerAliquotInvoiceGenerator implements InvoiceGenerator {


    @Override
    public Invoice generateInvoice(Priceable priceable) {
        Invoice invoice = new InvoiceImpl(priceable,"Foo");
        final Map<StartingSample,Collection<Quote>> quotesForAliquots = new HashMap<StartingSample,Collection<Quote>>();

        // build a list of quotes for each aliquot in the sample sheet,
        // eliminating redundant aliquots and quotes along the way

        for (SampleInstance sampleInstance: priceable.getSampleInstances()) {
            if (!quotesForAliquots.containsKey(sampleInstance.getStartingSample())) {
                quotesForAliquots.put(sampleInstance.getStartingSample(),new HashSet<Quote>());
            }
            Project p = sampleInstance.getProject();
            quotesForAliquots.get(sampleInstance.getStartingSample()).addAll(p.getAvailableQuotes());
        }

        for (Map.Entry<StartingSample,Collection<Quote>> quotesForAliquot: quotesForAliquots.entrySet()) {
            invoice.add(new InvoiceLineItemImpl(quotesForAliquot.getKey(),quotesForAliquot.getValue()));
        }
        return invoice;
    }
}
