package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.inject.Inject;
import javax.ws.rs.Produces;

/**
 * TODO scottmat fill in javadoc!!!
 */
public class SapIntegrationClientProducer {

    @Inject
    private Deployment deployment;

    private static SapIntegrationClient testInstance;

    public static SapIntegrationClient testInstance() {

        if(testInstance == null) {
            synchronized (SapIntegrationClient.class) {
                if(testInstance == null) {
                    SapConfig sapConfig = SapConfig.produce(Deployment.DEV);
                    testInstance = new SapIntegrationClientImpl(sapConfig);
                }
            }
        }

        return testInstance;
    }

    public static SapIntegrationClient stubInstance() {
        return new SapIntegrationClientStub();
    }

    @Produces
    @Default
    @SessionScoped
    public SapIntegrationClient produce(@New SapIntegrationClientStub sapStub, @New SapIntegrationClientImpl sapImpl) {
        if(deployment == Deployment.STUBBY) {
            return sapStub;
        }

        return sapImpl;
    }
}
