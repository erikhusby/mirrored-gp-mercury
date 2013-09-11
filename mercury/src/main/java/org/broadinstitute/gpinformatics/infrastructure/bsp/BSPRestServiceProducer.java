package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.STUBBY;

public class BSPRestServiceProducer {

    @Inject
    private Deployment deployment;

    @Produces
    @Default
    @SessionScoped
    public BSPRestService produce(@New BSPRestServiceStub stub, @New BSPRestServiceImpl impl) {

        if (deployment == STUBBY) {
            return stub;
        }

        return impl;
    }

}
