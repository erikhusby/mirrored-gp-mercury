package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/** Produces real implementation or a stub implementation for testing. */
public class MercuryClientProducer {

    @Inject
    private Deployment deployment;

    @Produces
    @Default
    @SessionScoped
    public MercuryClientService produce(@New MercuryClientServiceStub stub, @New MercuryClientServiceImpl impl) {
        return (deployment == Deployment.STUBBY ? stub : impl);
    }

    public static MercuryClientService stubInstance() {
        return new MercuryClientServiceStub();
    }

}
