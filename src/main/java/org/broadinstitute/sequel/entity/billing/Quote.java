package org.broadinstitute.sequel.entity.billing;

import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.sample.StartingSample;

import java.util.Collection;

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

    /**
     * Adds the given work summary to this quote.  Think of this as saying
     * "These samples (#samples) represent #numWorkItems work items
     * in quote server work batch id #quoteServerBatchId, which was
     * issued on behalf of lab event #event."
     * @param samples the list of samples
     * @param event the event that triggered the work
     * @param quoteServerBatchId the batch id assigned by the quote server
     */
    public void addWorkItem(Collection<StartingSample> samples,
                            double numWorkItems,
                            LabEvent event,
                            String quoteServerBatchId) {
        throw new RuntimeException("Not implemented");
    }
}
