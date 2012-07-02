package org.broadinstitute.sequel.entity.billing;

import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.project.BasicProjectPlan;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.infrastructure.quote.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

// todo jmt factories should be in control
/**
 * Factory that creates instances of {@link Billable}
 * for a given {@link LabEvent}.  The created {@link Billable}
 * logs work in the quote server once per {@link org.broadinstitute.sequel.entity.sample.StartingSample}.
 * This can be very different from billing per {@link SampleInstance}.
 * 
 * For instance, if you have a single {@link StartingSample} in
 * three different wells of a plate, and each of those different
 * wells has a different {@link org.broadinstitute.sequel.entity.vessel.MolecularEnvelope},
 * then billing by {@link SampleInstance} will result in three
 * different bills, whereas billing by {@link StartingSample} will
 * result in just a single bill.
 */
public class PerSampleBillableFactory {

    /**
     * Creates a new {@link Billable} for the given {@link LabEvent}.
     * When {@link Billable#doBilling(org.broadinstitute.sequel.infrastructure.quote.QuoteService)} is
     * called, {@link LabEvent} will have its {@link org.broadinstitute.sequel.entity.labevent.LabEvent#getQuoteServerBatchId()} updated
     * with the batch id returned by {@link QuoteService#registerNewWork(org.broadinstitute.sequel.infrastructure.quote.Quote, org.broadinstitute.sequel.infrastructure.quote.PriceItem, double, String, String, String)}.
     * @param event
     * @return
     */
    public static Billable createBillable(final LabEvent event) {
        final Map<QuotePriceItem,Double> workItemsPerQuote = new HashMap<QuotePriceItem, Double>();
        final Collection<StartingSample> samplesBilledAlready = new HashSet<StartingSample>();
        for (LabVessel source : event.getSourceLabVessels()) {
            for (SampleInstance sampleInstance : source.getSampleInstances()) {
                ProjectPlan projectPlan = sampleInstance.getSingleProjectPlan();
                StartingSample sample = sampleInstance.getStartingSample();
                
                PriceItem priceItem = projectPlan.getWorkflowDescription().getPriceItem();
                org.broadinstitute.sequel.infrastructure.quote.Quote quote = projectPlan.getQuoteDTO();
                if (quote != null) {
                    /** do we use true sample (aliquot) here, or {@link org.broadinstitute.sequel.entity.project.SampleAnalysisBuddies}? */

                    if (PriceItem.SAMPLE_UNITS.equalsIgnoreCase(priceItem.getUnits())) {
                        // todo this is a transaction problem.  instead send a CDI event
                        // out here and when the sequel transaction completes,
                        // have the event processor post the changes to quote server
                        if (!samplesBilledAlready.contains(sample)) {
                            QuotePriceItem quotePriceItem = new QuotePriceItem(quote,priceItem);
                            if (!workItemsPerQuote.containsKey(quotePriceItem)) {
                                workItemsPerQuote.put(quotePriceItem,1d);
                            }
                            else {
                                double workItems = workItemsPerQuote.remove(quotePriceItem);
                                workItemsPerQuote.put(quotePriceItem,++workItems);
                            }
                            samplesBilledAlready.add(sample);

                            // todo need a relationship persisted between Quote entity,
                            // sample, and event, so that people can review the billing
                            // information
                        }
                    }
                    else {
                        throw new RuntimeException("I don't know how to bill in " + priceItem.getUnits() + " units.");
                    }
                }
            }
        }
        Billable billable = new Billable() {
            /**
             * This probably needs to be annotated transactional
             * @param quoteService
             */
            @Override
            public void doBilling(QuoteService quoteService) {
                for (Map.Entry<QuotePriceItem, Double> itemsPerQuote : workItemsPerQuote.entrySet()) {
                    org.broadinstitute.sequel.infrastructure.quote.Quote quote = itemsPerQuote.getKey().getQuote();
                    PriceItem priceItem = itemsPerQuote.getKey().getPriceItem();
                    // todo callback url...need a UI that shows the event detail so that quote server
                    // can link to it during drill down.
                    // see http://quoteqa:8080/quotes/quote/Quote.action?quote.id=2102&viewQuote=&__fsk=-1786926860
                    // and hit the "view all work items" button for an example.
                    String batchId = quoteService.registerNewWork(quote,priceItem,itemsPerQuote.getValue(),null,null,null);
                    event.setQuoteServerBatchId(batchId);
                }
            }
        };
        return billable;
    }
}
