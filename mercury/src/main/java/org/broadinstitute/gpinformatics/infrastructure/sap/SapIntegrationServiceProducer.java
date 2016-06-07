package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class SapIntegrationServiceProducer {

    private Deployment deployment;

    private static SapIntegrationService testInstance;

    public static SapIntegrationService testInstance() {

        if(testInstance == null) {
            synchronized (SapIntegrationService.class) {
                if(testInstance == null) {
                    SapConfig sapConfig = SapConfig.produce(Deployment.DEV);
                    testInstance = new SapIntegrationServiceImpl(sapConfig);
                }
            }
        }
        return testInstance;
    }

    @Inject
    public SapIntegrationServiceProducer(Deployment deployment) {
        this.deployment = deployment;
    }

    public static SapIntegrationService stubInstance() {
        return new SapIntegrationClientStub();
    }

    @Produces
    @Default
    @RequestScoped
    public SapIntegrationService produce(@New SapIntegrationClientStub sapStub, @New SapIntegrationServiceImpl svc) {
        System.out.println("In the produce method for SAP Integration Service Producer");
        if(deployment == Deployment.STUBBY) {
            return sapStub;
        }

        return svc;
    }
}
