package org.broadinstitute.sequel.infrastructure.bsp.plating;


import org.broadinstitute.sequel.infrastructure.bsp.BSPConfig;
import org.broadinstitute.sequel.infrastructure.bsp.BSPConfigProducer;
import org.broadinstitute.sequel.infrastructure.deployment.Deployment;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.sequel.infrastructure.deployment.Deployment.QA;
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


    /**
     * Creates a BSPPlatingRequestServiceImpl with plain old new operator for container-free testing,
     * not a managed bean!
     *
     * @return
     */
    public static BSPPlatingRequestService qaInstance() {

        BSPConfig bspConfig = BSPConfigProducer.produce(QA);

        return new BSPPlatingRequestServiceImpl(bspConfig);

    }
}
