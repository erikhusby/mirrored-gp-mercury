package org.broadinstitute.gpinformatics.infrastructure.salesforce;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class SalesforceServiceProducer {

    @Inject
    private Deployment deployment;

    public static SalesforceService testInstance() {

        SalesforceConfig salesforceConfig = SalesforceConfig.produce(Deployment.DEV);

        return new SalesforceServiceImpl(salesforceConfig);
    }

    public static SalesforceService stubInstance() { return new SalesforceServiceStub();}

    @Produces
    @Default
    public SalesforceService produce(@New SalesforceServiceStub stub, @New SalesforceServiceImpl impl) {

        if(deployment == Deployment.STUBBY) {

            return stub;
        }
        return impl;
    }

}

