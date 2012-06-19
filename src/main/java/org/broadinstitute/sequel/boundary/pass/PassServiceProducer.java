package org.broadinstitute.sequel.boundary.pass;

import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.control.pass.PassService;
import org.broadinstitute.sequel.infrastructure.deployment.Deployment;
import org.broadinstitute.sequel.infrastructure.deployment.Impl;
import org.broadinstitute.sequel.infrastructure.deployment.Stub;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;


/**
 * Class to produce instances of {@link PassService} appropriate to the current {@link Deployment}.  Arquillian
 * micro-deployments should be running in a {@link Deployment#STUBBY} deployment which will yield a
 * {@link PassServiceStub} implementation, while non-STUBBY deployments will get
 * {@link PassSOAPServiceImpl} implementations configured to point to the correct underlying Squid instance.
 */
public class PassServiceProducer {


    @Inject
    private Log log;


    @Inject
    private Deployment deployment;


    @Inject
    @Impl
    private PassService impl;


    @Inject
    @Stub
    private PassService stub;



    @Produces
    @Default
    public PassService produce() {

        if (deployment == Deployment.STUBBY) {
            log.info("STUBBY deployment, returning stub");
            return stub;
        }
        else {
            log.info("Non-STUBBY deployment, returning impl");
            return impl;
        }


    }
}
