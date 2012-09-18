package org.broadinstitute.gpinformatics.mercury.boundary.pmbridge;


import org.broadinstitute.gpinformatics.mercury.infrastructure.deployment.Deployment;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.mercury.infrastructure.deployment.Deployment.STUBBY;

public class PMBridgeServiceProducer {

    @Inject
    private Deployment deployment;



    public static PMBridgeService stubInstance() {
        return new PMBridgeServiceStub();
    }



    @Produces
    @Default
    @SessionScoped
    public PMBridgeService produce(@New PMBridgeServiceStub stub, @New PMBridgeServiceImpl impl) {

        if ( deployment == STUBBY )
            return stub;

        return impl;

    }


}
