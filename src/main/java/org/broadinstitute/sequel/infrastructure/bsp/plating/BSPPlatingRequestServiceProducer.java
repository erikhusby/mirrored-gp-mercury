package org.broadinstitute.sequel.infrastructure.bsp.plating;


import org.broadinstitute.sequel.infrastructure.deployment.Deployment;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.sequel.infrastructure.deployment.Deployment.STUBBY;

public class BSPPlatingRequestServiceProducer {

    @Inject
    private Deployment deployment;

    @Produces
    @Default
    @SessionScoped
    public BSPPlatingRequestService produce(@New BSPPlatingRequestServiceStub stub, @New BSPPlatingRequestServiceImpl impl) {

        if (deployment == STUBBY)
            return stub;

        return impl;
    }


}
