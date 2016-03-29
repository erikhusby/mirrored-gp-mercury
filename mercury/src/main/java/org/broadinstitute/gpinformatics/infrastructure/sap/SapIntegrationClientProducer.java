package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.sapservices.SapIntegrationClient;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.inject.Inject;
import javax.ws.rs.Produces;

public class SapIntegrationClientProducer {

    private Deployment deployment;

    @Inject
    private SapConfig sapConfig;

    private static SapIntegrationClient testInstance;

    public static SapIntegrationClient testInstance() {

        if(testInstance == null) {
            synchronized (SapIntegrationClient.class) {
                if(testInstance == null) {
                    SapConfig sapConfig = SapConfig.produce(Deployment.DEV);
                    testInstance = new SapIntegrationSvc(sapConfig);
                }
            }
        }

        return testInstance;
    }

    @Inject
    public SapIntegrationClientProducer(Deployment deployment) {
        this.deployment = deployment;
    }

    public static SapIntegrationClient stubInstance() {
        return new SapIntegrationClientStub();
    }

    @Produces
    @Default
    @RequestScoped
    public SapIntegrationClient produce(@New SapIntegrationClientStub sapStub) {
        if(deployment == Deployment.STUBBY) {
            return sapStub;
        }

        return new SapIntegrationSvc(sapConfig);
    }
}
