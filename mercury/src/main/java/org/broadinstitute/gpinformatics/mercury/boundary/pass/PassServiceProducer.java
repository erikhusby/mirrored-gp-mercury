package org.broadinstitute.gpinformatics.mercury.boundary.pass;

import org.broadinstitute.gpinformatics.mercury.control.pass.PassService;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.TestInstance;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConfig;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConfigProducer;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.STUBBY;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.TEST;


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
    public PassService testInstance() {
        return new PassServiceImpl( TEST );
    }


    public static PassService stubInstance() {
        return new PassServiceStub();
    }



    @Produces
    @Default
    @SessionScoped
    public PassService produce(@New PassServiceStub stub, @New PassServiceImpl impl) {

        if ( deployment == STUBBY )
            return stub;

        return impl;
    }


    public static PassService produce(Deployment deployment) {

        if ( deployment == STUBBY )
            return new PassServiceStub();

        final SquidConfig squidConfig =
                SquidConfigProducer.getConfig(deployment);

        return new PassServiceImpl(squidConfig);
    }



}
