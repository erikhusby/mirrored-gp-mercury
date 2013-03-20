package org.broadinstitute.gpinformatics.mercury.entity.billing;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collection;

/**
 * Quote entity, which is just a alphanumeric id
 * and a wrapper around a quote DTO for more
 * details.
 */
@Entity
@Audited
@Table(schema = "mercury")
public class Quote {

    @Transient
    private org.broadinstitute.gpinformatics.infrastructure.quote.Quote quoteDTO;

    @Id
    private String alphanumericId;

    /**
     * @param alphanumericId the alphanumeric id of the quote,
     *                       to be persisted in mercury
     * @param quoteDTO everything else about the quote,
     *                 fetched from the quote server
     */
    public Quote(String alphanumericId,
                 org.broadinstitute.gpinformatics.infrastructure.quote.Quote quoteDTO) {
        this.quoteDTO = quoteDTO;
        this.alphanumericId = alphanumericId;
    }

    protected Quote() {
    }

    public String getAlphanumericId() {
        return alphanumericId;
    }

    public org.broadinstitute.gpinformatics.infrastructure.quote.Quote getQuote() {
        return quoteDTO;
    }

}
