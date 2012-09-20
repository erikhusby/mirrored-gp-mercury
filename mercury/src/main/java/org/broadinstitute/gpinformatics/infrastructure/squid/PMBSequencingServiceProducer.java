package org.broadinstitute.gpinformatics.infrastructure.squid;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.STUBBY;

public class PMBSequencingServiceProducer {

    @Inject
    private Deployment deployment;


    // TODO: make selection dynamic to avoid error from ThriftConfigProducer when deploying to a STUBBY container
    @Produces
    @Default
    @SessionScoped
    public PMBSequencingService produce(@New PMBSequencingServiceImpl impl) {

        if ( deployment == STUBBY )
            return null;

        return impl;

    }
}
