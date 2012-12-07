package org.broadinstitute.gpinformatics.infrastructure.bettalims;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.pmbridge.AbstractConfigProducer;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * CDI producer for BettaLIMS configuration
 */
public class BettalimsConfigProducer extends AbstractConfigProducer<BettalimsConfig> {
    @Inject
    private Deployment deployment;


    @Produces
    @Default
    public BettalimsConfig produce() {
        return produce(deployment);
    }

    public static BettalimsConfig getConfig(Deployment deployment) {
        return new BettalimsConfigProducer().produce(deployment);
    }
}
