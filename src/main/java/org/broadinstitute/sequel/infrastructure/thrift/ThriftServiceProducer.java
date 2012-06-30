package org.broadinstitute.sequel.infrastructure.thrift;


import org.broadinstitute.sequel.infrastructure.deployment.Deployment;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.sequel.infrastructure.deployment.Deployment.STUBBY;

public class ThriftServiceProducer {


    @Inject
    private Deployment deployment;



    @Produces
    @Default
    public ThriftService produce() {

        return produce( deployment );

    }


    public static ThriftService produce( Deployment deployment ) {

        if ( deployment == STUBBY )

            return new OfflineThriftService();

        ThriftConfig thriftConfig = ThriftConfigProducer.produce( deployment );

        return new LiveThriftService(thriftConfig);


    }

}
