package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.TestInstance;
import org.broadinstitute.gpinformatics.infrastructure.pmbridge.AbstractConfigProducer;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class BSPConfigProducer extends AbstractConfigProducer<BSPConfig> {

    @Inject
    private Deployment deployment;

    @Produces
    @TestInstance
    public BSPConfig testInstance() {
        return produce(Deployment.DEV);
    }

    @Produces
    public BSPConfig produce() {
        return produce(deployment);
    }

    public static BSPConfig getConfig(Deployment deployment) {
        return new BSPConfigProducer().produce(deployment);
    }
}
