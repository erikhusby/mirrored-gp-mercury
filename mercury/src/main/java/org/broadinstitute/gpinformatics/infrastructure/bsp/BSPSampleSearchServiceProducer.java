package org.broadinstitute.gpinformatics.infrastructure.bsp;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * <strong>NOT a CDI producer!</strong><br/>
 * Produces stub and test instances for testing purposes only
 */
public class BSPSampleSearchServiceProducer {

    /**
     * Creates a BSPSampleSearchService stub instance with plain old new operator for container-free testing.
     */
    public static BSPSampleSearchService stubInstance() {
        return new BSPSampleSearchServiceStub();
    }

    /**
     * Creates a BSPSampleSearchService pointing to DEV with plain old new operator for container-free testing.
     */
    public static BSPSampleSearchService testInstance() {
        BSPConfig bspConfig = BSPConfig.produce(DEV);
        return new BSPSampleSearchServiceImpl(bspConfig);
    }
}
