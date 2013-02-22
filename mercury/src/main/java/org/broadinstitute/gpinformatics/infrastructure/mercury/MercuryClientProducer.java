package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * @author epolk
 * Produces a stub implementation of Mercury services for testing.
 */
public class MercuryClientProducer {

    @Inject
    private Deployment deployment;

    @Produces
    @Default
    @SessionScoped
    public MercuryClientService produce(@New MercuryClientServiceStub stub, @New MercuryClientServiceImpl impl) {

        if(deployment == Deployment.STUBBY)
            return stub;

        return impl;
    }

    public static MercuryClientService stubInstance() {
        return new MercuryClientServiceStub();
    }

}
