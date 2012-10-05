package org.broadinstitute.gpinformatics.infrastructure.bsp.plating;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfigProducer;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.STUBBY;

public class BSPManagerFactoryProducer {

    @Inject
    private Deployment deployment;

    @Produces
    @Default
    @SessionScoped
    public BSPManagerFactory produce(@New BSPManagerFactoryStub stub, @New BSPManagerFactoryImpl impl) {
        if (deployment == STUBBY) {
            return stub;
        }
        return impl;
    }

    /**
     * Copied from BSPCohortSearchServiceImpl.
     */
    public static BSPManagerFactory qaInstance() {
        return new BSPManagerFactoryImpl(BSPConfigProducer.getConfig(Deployment.DEV));
    }
}
