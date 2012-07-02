package org.broadinstitute.sequel.infrastructure.bsp;


import org.broadinstitute.sequel.infrastructure.deployment.Deployment;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.sequel.infrastructure.deployment.Deployment.QA;
import static org.broadinstitute.sequel.infrastructure.deployment.Deployment.STUBBY;

public class BSPSampleSearchServiceProducer {

    @Inject
    private Deployment deployment;


    @Produces
    @Default
    @SessionScoped
    public BSPSampleSearchService produce(@New BSPSampleSearchServiceStub stub, @New BSPSampleSearchServiceImpl impl) {

        if ( deployment == STUBBY )
            return stub;

        return impl;
    }


    public static BSPSampleSearchService qaInstance() {

        BSPConfig bspConfig = BSPConfigProducer.produce(QA);

        return new BSPSampleSearchServiceImpl( bspConfig );

    }
}
