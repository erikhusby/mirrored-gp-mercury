package org.broadinstitute.sequel.boundary.pmbridge;


import org.broadinstitute.sequel.infrastructure.deployment.Deployment;
import org.broadinstitute.sequel.infrastructure.deployment.StubInstance;
import org.broadinstitute.sequel.infrastructure.deployment.TestInstance;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.sequel.infrastructure.deployment.Deployment.STUBBY;
import static org.broadinstitute.sequel.infrastructure.deployment.Deployment.TEST;

public class PMBridgeServiceProducer {

    @Inject
    private Deployment deployment;


    @Produces
    @TestInstance
    public PMBridgeService testInstance() {
        return produce( TEST );
    }


    @Produces
    @StubInstance
    public PMBridgeService stubInstance() {
        return produce( STUBBY );
    }


    public static PMBridgeService produceStub() {
        return new PMBridgeServiceProducer().stubInstance();
    }


    @Produces
    @Default
    public PMBridgeService produce() {

        return produce( deployment );

    }


    public static PMBridgeService produce( Deployment deployment ) {

        if ( deployment == STUBBY )
            return new PMBridgeServiceStub();

        return new PMBridgeServiceImpl( deployment );
    }


}
