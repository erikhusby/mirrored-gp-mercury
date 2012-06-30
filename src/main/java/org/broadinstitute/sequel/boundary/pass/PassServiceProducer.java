package org.broadinstitute.sequel.boundary.pass;

import org.broadinstitute.sequel.control.pass.PassService;
import org.broadinstitute.sequel.infrastructure.deployment.Deployment;
import org.broadinstitute.sequel.infrastructure.deployment.TestInstance;
import org.broadinstitute.sequel.infrastructure.squid.SquidConfig;
import org.broadinstitute.sequel.infrastructure.squid.SquidConfigProducer;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.sequel.infrastructure.deployment.Deployment.STUBBY;
import static org.broadinstitute.sequel.infrastructure.deployment.Deployment.TEST;


/**
 * Class to produce instances of {@link PassService} appropriate to the current {@link Deployment}.  Arquillian
 * micro-deployments should be running in a {@link Deployment#STUBBY} deployment which will yield a
 * {@link PassServiceStub} implementation, while non-STUBBY deployments will get
 * {@link PassServiceImpl} implementations configured to point to the correct underlying Squid instance.
 */
public class PassServiceProducer {


    @Inject
    private Deployment deployment;


    @Produces
    @TestInstance
    /**
     * This is currently the only explicitly qualified producer required for this service, if others are needed for
     * DEV, QA, PROD, or STUBBY they can be added similarly
     */
    public PassService testInstance() {
        return new PassServiceImpl(TEST);
    }


    @Produces
    @Default
    public PassService produce() {

        return produce( deployment );

    }


    public static PassService produce(Deployment deployment) {

        if ( deployment == STUBBY )
            return new PassServiceStub();

        final SquidConfig squidConfig =
                SquidConfigProducer.produce(deployment);

        return new PassServiceImpl(squidConfig);
    }


    public static PassService produceStub() {
        return produce(STUBBY);
    }


}
