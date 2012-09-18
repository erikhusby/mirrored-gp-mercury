package org.broadinstitute.gpinformatics.mercury.infrastructure.quote;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
/**
 * This class executes billing whenever 
 * {@link Billable} events are fired,
 * from {@link org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler}
 * for instance.
 */
public class Biller {

    @Inject QuoteService quoteService;

    /**
     * Handle the {@link Billable} by using
     * the injected #quoteService
     * @param billable
     */
    public void processBill(@Observes Billable billable) {
        System.out.println("Charging for " + billable);
        billable.doBilling(quoteService);
    }

    /**
     * Used for overriding the injected service during
     * unit tests.  Is there a better way to do this?
     * @param quoteService
     */
    public void setQuoteService(QuoteService quoteService) {
        this.quoteService = quoteService;
    }
}
