package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.STUBBY;

/**
 */
public class BSPSampleReceiptServiceProducer {


    @Inject
    private Deployment deployment;

    @Produces
    @Default
    @ApplicationScoped
    public BSPSampleReceiptService produce(@New BSPSampleReceiptServiceStub stub, @New BSPSampleReceiptServiceImpl impl) {

        if (deployment == STUBBY) {
            return stub;
        }

        return impl;
    }

    public static BSPSampleReceiptService stubInstance() {
        return new BSPSampleReceiptServiceStub();
    }


    /**
     * Creates a BSPCohortSearchService with plain old new operator for container-free testing,
     * not a managed bean!
     *
     * @return A new BSPCohortSearchService to be used for testing.
     */
    public static BSPSampleReceiptService testInstance() {

        BSPConfig bspConfig = BSPConfig.produce(DEV);

        return new BSPSampleReceiptServiceImpl(bspConfig);
    }
}
