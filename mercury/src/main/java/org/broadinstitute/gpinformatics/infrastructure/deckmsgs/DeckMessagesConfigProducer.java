package org.broadinstitute.gpinformatics.infrastructure.deckmsgs;

import org.broadinstitute.gpinformatics.infrastructure.pmbridge.AbstractConfigProducer;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * CDI Producer
 */
public class DeckMessagesConfigProducer extends AbstractConfigProducer<DeckMessagesConfig> {

    @Inject
    private Deployment deployment;

    @Produces
    @Default
    public DeckMessagesConfig produce() {
        return produce( deployment );
    }

    public static DeckMessagesConfig getConfig( Deployment deployment ) {
        return new DeckMessagesConfigProducer().produce( deployment );
    }
}
