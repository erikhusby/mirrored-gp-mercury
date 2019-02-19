package org.broadinstitute.gpinformatics.infrastructure.bsp.plating;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * @author Scott Matthews
 *         Date: 11/26/12
 *         Time: 2:58 PM
 */
@ApplicationScoped
public class BSPManagerFactoryProducer {

    @Inject
    private Deployment deployment;

    @Inject
    public BSPManagerFactoryProducer( BSPConfig bspConfig ) {
        runtimeInstance = new BSPManagerFactoryImpl(bspConfig);
    };

    private BSPManagerFactory runtimeInstance;

    private static BSPManagerFactory testInstance;

    /**
     * NOT CDI compliant!  Use for external integration testing only
     * @return
     */
    public static BSPManagerFactory testInstance() {
        if (testInstance == null) {
            synchronized (BSPManagerFactory.class) {
                if (testInstance == null) {
                    BSPConfig bspTestConfig = BSPConfig.produce(Deployment.TEST);
                    testInstance = new BSPManagerFactoryImpl(bspTestConfig);
                }
            }
        }

        return testInstance;

    }

    /**
     * NOT CDI compliant!  Use for DBFree testing only
     * @return
     */
    public static BSPManagerFactory stubInstance() {
        return new BSPManagerFactoryStub();
    }

    @Produces
    @Default
    @ApplicationScoped
    public BSPManagerFactory produce() {

        if (deployment == Deployment.STUBBY) {
            return new BSPManagerFactoryStub();
        }

        return runtimeInstance;

    }
}
