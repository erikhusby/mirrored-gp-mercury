package org.broadinstitute.gpinformatics.mercury.boundary.designation;

import org.broadinstitute.gpinformatics.mercury.infrastructure.deployment.Deployment;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.mercury.infrastructure.deployment.Deployment.STUBBY;


/**
 * Class to produce instances of {@link LibraryRegistrationSOAPService} appropriate to the current {@link org.broadinstitute.gpinformatics.mercury.infrastructure.deployment.Deployment}.  Arquillian
 * micro-deployments should be running in a {@link org.broadinstitute.gpinformatics.mercury.infrastructure.deployment.Deployment#STUBBY} deployment which will yield a
 * {@link LibraryRegistrationSOAPServiceStub} implementation, while non-STUBBY deployments will get
 * {@link LibraryRegistrationSOAPServiceImpl} implementations configured to point to the correct underlying Squid instance.
 */
public class LibraryRegistrationSOAPServiceProducer {


    @Inject
    private Deployment deployment;


    public static LibraryRegistrationSOAPService stubInstance() {
        return new LibraryRegistrationSOAPServiceStub();
    }



    @Produces
    @Default
    @SessionScoped
    public LibraryRegistrationSOAPService produce(
            @New LibraryRegistrationSOAPServiceStub stub, @New LibraryRegistrationSOAPServiceImpl impl) {

        if ( deployment == STUBBY )
            return stub;

        return impl;

    }


}
