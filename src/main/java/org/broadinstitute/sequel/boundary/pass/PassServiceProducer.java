package org.broadinstitute.sequel.boundary.pass;

import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.control.pass.PassService;
import org.broadinstitute.sequel.infrastructure.deployment.Deployment;
import org.broadinstitute.sequel.infrastructure.deployment.Impl;
import org.broadinstitute.sequel.infrastructure.squid.SquidConfiguration;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;


public class PassServiceProducer {

    @Inject
    private Deployment deployment;

    @Inject
    private SquidConfiguration squidConfiguration;

    @Inject
    @Impl
    // We need the {@link @Impl} qualifier or we get errors with multiple {@link @Default} implementations
    // to @Inject PassService injection points,
    // and putting {@link @Alternative} on {@link PassSOAPServiceImpl} makes it impossible to inject instances of that
    // class here
    private PassSOAPServiceImpl impl;

    @Inject
    private Log log;


    // The instance we'll return to our @Injection points
    private PassService instance;



    @Produces
    @Default
    public PassService produce() {

        if (instance == null) {

            if (deployment == Deployment.STUBBY) {
                log.info("Creating PassServiceStub for STUBBY deployment");
                instance = new PassServiceStub();
            }
            else {
                log.info("Non-STUBBY deployment, instance = impl");
                instance = impl;

            }

        }

        return instance;

    }
}
