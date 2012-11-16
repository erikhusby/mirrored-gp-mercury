package org.broadinstitute.gpinformatics.infrastructure.deployment;

import org.broadinstitute.gpinformatics.infrastructure.pmbridge.AbstractConfigProducer;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * @author breilly
 */
public class MercuryConfigProducer extends AbstractConfigProducer<MercuryConfig> {

    @Inject
    private Deployment deployment;

    @Produces
    public MercuryConfig produce() {
        return produce(deployment);
    }
}
