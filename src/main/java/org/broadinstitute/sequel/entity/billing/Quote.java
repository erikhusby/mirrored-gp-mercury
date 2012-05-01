package org.broadinstitute.sequel.entity.billing;

import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.sample.StartingSample;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.util.Collection;

/**
 * Quote entity, which is just a alphanumeric id
 * and a wrapper around a quote DTO for more
 * details.
 */
@Entity
public class Quote {

    @Transient
    private org.broadinstitute.sequel.infrastructure.quote.Quote quoteDTO;

    @Id
    private String alphanumericId;

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

    protected Quote() {
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
