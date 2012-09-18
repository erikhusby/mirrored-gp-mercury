package org.broadinstitute.gpinformatics.mercury.infrastructure.pmbridge;


import org.broadinstitute.gpinformatics.mercury.infrastructure.deployment.Deployment;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class PMBridgeConfigProducer extends AbstractConfigProducer<PMBridgeConfig> {


    @Inject
    private Deployment deployment;


    @Produces
    @Default
    public PMBridgeConfig produce() {
        return produce( deployment );
    }

    public static PMBridgeConfig getConfig( Deployment deployment ) {
        return new PMBridgeConfigProducer().produce( deployment );
    }


}
