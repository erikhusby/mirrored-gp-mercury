package org.broadinstitute.gpinformatics.infrastructure.bsp.plating;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * @author Scott Matthews
 *         Date: 11/26/12
 *         Time: 2:58 PM
 */
public class BSPManagerFactoryProducer {

    @Inject
    private Deployment deployment;

    private static BSPManagerFactory testInstance;

    public static BSPManagerFactory testInstance() {

        if (testInstance == null) {
            synchronized (BSPManagerFactory.class) {
                if (testInstance == null) {
                    BSPConfig bspConfig = BSPConfig.produce(Deployment.TEST);
                    testInstance = new BSPManagerFactoryImpl(bspConfig);
                }
            }
        }

        return testInstance;

    }

    public static BSPManagerFactory stubInstance() {

        return new BSPManagerFactoryStub();
    }

    @Produces
    @Default
    @ApplicationScoped
    public BSPManagerFactory produce(@New BSPManagerFactoryStub stub, @New BSPManagerFactoryImpl impl) {

        if (deployment == Deployment.STUBBY) {
            return stub;
        }

        return impl;

    }
}
