package org.broadinstitute.gpinformatics.infrastructure.thrift;


import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.STUBBY;

public class ThriftServiceProducer {


    @Inject
    private Deployment deployment;


    // TODO: make selection dynamic to avoid error from ThriftConfigProducer when deploying to a STUBBY container
    @Produces
    @Default
    @RequestScoped
    public ThriftService produce(@New OfflineThriftService stub, @New LiveThriftService impl) {

        if ( deployment == STUBBY )
            return stub;

        return impl;

    }


}
