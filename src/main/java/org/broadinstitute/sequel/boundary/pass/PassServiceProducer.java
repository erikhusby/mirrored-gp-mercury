package org.broadinstitute.sequel.boundary.pass;

import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.control.pass.PassService;
import org.broadinstitute.sequel.infrastructure.deployment.*;
import org.broadinstitute.sequel.infrastructure.squid.SquidConnectionParameters;
import org.broadinstitute.sequel.infrastructure.squid.SquidConnectionParametersProducer;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.sequel.infrastructure.deployment.Deployment.*;


/**
 * Class to produce instances of {@link PassService} appropriate to the current {@link Deployment}.  Arquillian
 * micro-deployments should be running in a {@link Deployment#STUBBY} deployment which will yield a
 * {@link PassServiceStub} implementation, while non-STUBBY deployments will get
 * {@link PassSOAPServiceImpl} implementations configured to point to the correct underlying Squid instance.
 */
public class PassServiceProducer implements InstanceSpecificProducer<PassService> {


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


    @Override
    @Produces
    @DevInstance
    public PassService devInstance() {
        return new PassSOAPServiceImpl(DEV);
    }

    @Override
    @Produces
    @TestInstance
    public PassService testInstance() {
        return new PassSOAPServiceImpl(TEST);
    }

    @Override
    @Produces
    @QAInstance
    public PassService qaInstance() {
        return new PassSOAPServiceImpl(QA);
    }

    @Override
    @Produces
    @ProdInstance
    public PassService prodInstance() {
        return new PassSOAPServiceImpl(PROD);
    }


    @Override
    @Produces
    @StubInstance
    public PassService stubInstance() {
        return new PassServiceStub();
    }


    @Produces
    @Default
    public PassService produce() {

        if (deployment == STUBBY) {
            log.info("STUBBY deployment, returning stub");
            return stub;
        }

        log.info("Non-STUBBY deployment, returning impl");
        return impl;

    }


    public static PassService produce(Deployment deployment) {

        if (deployment == STUBBY)
            return new PassServiceStub();

        final SquidConnectionParameters squidConnectionParameters =
                SquidConnectionParametersProducer.produce(deployment);

        return new PassSOAPServiceImpl(squidConnectionParameters);
    }


    public static PassService produceStub() {
        return PassServiceProducer.produce(STUBBY);
    }


    public static PassService testDeployment() {
        return produce(TEST);
    }
}
