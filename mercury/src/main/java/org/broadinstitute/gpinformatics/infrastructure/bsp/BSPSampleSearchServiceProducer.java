package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.STUBBY;

public class BSPSampleSearchServiceProducer {
    @Inject
    private Deployment deployment;

    @Produces
    @Default
    @SessionScoped
    public BSPSampleSearchService produce(@New BSPSampleSearchServiceStub stub, @New BSPSampleSearchServiceImpl impl) {

        if (deployment == STUBBY) {
            return stub;
        }

        return impl;
    }

    public static BSPSampleSearchService stubInstance() {
        return new BSPSampleSearchServiceStub();
    }

    /**
     * Creates a BSPSampleSearchService with plain old new operator for container-free testing,
     * not a managed bean!
     * <p/>
     * This is also needed to get a real copy of the BSP services in container testing, since by default
     * injection will give you a Stub BSP service, not an Impl.
     */
    public static BSPSampleSearchService testInstance() {

        BSPConfig bspConfig = BSPConfig.produce(DEV);

        return new BSPSampleSearchServiceImpl(bspConfig);
    }
}
