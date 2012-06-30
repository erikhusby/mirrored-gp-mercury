package org.broadinstitute.sequel.infrastructure.bsp;


import org.broadinstitute.sequel.infrastructure.deployment.Deployment;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class BSPSampleSearchServiceProducer {

    @Inject
    private Deployment deployment;


    @Produces
    @Default
    public BSPSampleSearchService produce() {
        return produce( deployment );
    }


    public static BSPSampleSearchService produce ( Deployment deployment ) {

        if ( deployment == Deployment.STUBBY )
            return null;


        BSPConfig bspConfig = BSPConfigProducer.produce( deployment );

        return new BSPSampleSearchServiceImpl( bspConfig );

    }
}
