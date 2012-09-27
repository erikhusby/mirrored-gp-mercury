package org.broadinstitute.gpinformatics.infrastructure.gap;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.pmbridge.AbstractConfigProducer;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class GAPConfigProducer extends AbstractConfigProducer<GAPConfig> {


    @Inject
    private Deployment deployment;


    @Produces
    public GAPConfig produce() {

        return produce(deployment);
    }


    public static GAPConfig getConfig(Deployment deployment) {
        return new GAPConfigProducer().produce(deployment);
    }


}
