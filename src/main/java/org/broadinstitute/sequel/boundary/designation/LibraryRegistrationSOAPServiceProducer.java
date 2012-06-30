package org.broadinstitute.sequel.boundary.designation;

import org.broadinstitute.sequel.infrastructure.deployment.Deployment;
import org.broadinstitute.sequel.infrastructure.deployment.TestInstance;
import org.broadinstitute.sequel.infrastructure.squid.SquidConnectionParameters;
import org.broadinstitute.sequel.infrastructure.squid.SquidConnectionParametersProducer;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.sequel.infrastructure.deployment.Deployment.STUBBY;
import static org.broadinstitute.sequel.infrastructure.deployment.Deployment.TEST;


/**
 * Class to produce instances of {@link LibraryRegistrationSOAPService} appropriate to the current {@link org.broadinstitute.sequel.infrastructure.deployment.Deployment}.  Arquillian
 * micro-deployments should be running in a {@link org.broadinstitute.sequel.infrastructure.deployment.Deployment#STUBBY} deployment which will yield a
 * {@link LibraryRegistrationSOAPServiceStub} implementation, while non-STUBBY deployments will get
 * {@link LibraryRegistrationSOAPServiceImpl} implementations configured to point to the correct underlying Squid instance.
 */
public class LibraryRegistrationSOAPServiceProducer {


    @Inject
    private Deployment deployment;


    @Produces
    @TestInstance
    public LibraryRegistrationSOAPService testInstance() {

        return produce( TEST );

    }


    @Produces
    @Default
    public LibraryRegistrationSOAPService produce() {

        return produce( deployment );

    }


    public static LibraryRegistrationSOAPService produce(Deployment deployment) {

        if (deployment == STUBBY)
            return new LibraryRegistrationSOAPServiceStub();

        final SquidConnectionParameters squidConnectionParameters =
                SquidConnectionParametersProducer.produce(deployment);

        return new LibraryRegistrationSOAPServiceImpl(squidConnectionParameters);
    }


}
