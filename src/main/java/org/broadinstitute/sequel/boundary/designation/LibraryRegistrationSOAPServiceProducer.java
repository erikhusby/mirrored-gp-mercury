package org.broadinstitute.sequel.boundary.designation;

import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.infrastructure.deployment.Deployment;
import org.broadinstitute.sequel.infrastructure.deployment.DevInstance;
import org.broadinstitute.sequel.infrastructure.deployment.Impl;
import org.broadinstitute.sequel.infrastructure.deployment.InstanceSpecificProducer;
import org.broadinstitute.sequel.infrastructure.deployment.ProdInstance;
import org.broadinstitute.sequel.infrastructure.deployment.QAInstance;
import org.broadinstitute.sequel.infrastructure.deployment.Stub;
import org.broadinstitute.sequel.infrastructure.deployment.StubInstance;
import org.broadinstitute.sequel.infrastructure.deployment.TestInstance;
import org.broadinstitute.sequel.infrastructure.squid.SquidConnectionParameters;
import org.broadinstitute.sequel.infrastructure.squid.SquidConnectionParametersProducer;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.sequel.infrastructure.deployment.Deployment.*;


/**
 * Class to produce instances of {@link org.broadinstitute.sequel.control.pass.PassService} appropriate to the current {@link org.broadinstitute.sequel.infrastructure.deployment.Deployment}.  Arquillian
 * micro-deployments should be running in a {@link org.broadinstitute.sequel.infrastructure.deployment.Deployment#STUBBY} deployment which will yield a
 * {@link org.broadinstitute.sequel.boundary.pass.PassServiceStub} implementation, while non-STUBBY deployments will get
 * {@link org.broadinstitute.sequel.boundary.pass.PassSOAPServiceImpl} implementations configured to point to the correct underlying Squid instance.
 */
public class LibraryRegistrationSOAPServiceProducer implements InstanceSpecificProducer<LibraryRegistrationSOAPService> {


    @Inject
    private Log log;


    @Inject
    private Deployment deployment;


    @Inject
    @Impl
    private LibraryRegistrationSOAPService impl;


    @Inject
    @Stub
    private LibraryRegistrationSOAPService stub;


    @Override
    @Produces
    @DevInstance
    public LibraryRegistrationSOAPService devInstance() {
        return new LibraryRegistrationSOAPServiceImpl(DEV);
    }

    @Override
    @Produces
    @TestInstance
    public LibraryRegistrationSOAPService testInstance() {
        return new LibraryRegistrationSOAPServiceImpl(TEST);
    }

    @Override
    @Produces
    @QAInstance
    public LibraryRegistrationSOAPService qaInstance() {
        return new LibraryRegistrationSOAPServiceImpl(QA);
    }

    @Override
    @Produces
    @ProdInstance
    public LibraryRegistrationSOAPService prodInstance() {
        return new LibraryRegistrationSOAPServiceImpl(PROD);
    }


    @Override
    @Produces
    @StubInstance
    public LibraryRegistrationSOAPService stubInstance() {
        return stub;
    }


    @Produces
    @Default
    public LibraryRegistrationSOAPService produce() {

        if (deployment == STUBBY) {
            log.info("STUBBY deployment, returning stub");
            return stub;
        }

        log.info("Non-STUBBY deployment, returning impl");
        return impl;

    }


    public static LibraryRegistrationSOAPService produce(Deployment deployment) {

        if (deployment == STUBBY)
            return new LibraryRegistrationSOAPServiceStub();

        final SquidConnectionParameters squidConnectionParameters =
                SquidConnectionParametersProducer.produce(deployment);

        return new LibraryRegistrationSOAPServiceImpl(squidConnectionParameters);
    }


    public static LibraryRegistrationSOAPService testDeployment() {
        return produce(TEST);
    }
}
