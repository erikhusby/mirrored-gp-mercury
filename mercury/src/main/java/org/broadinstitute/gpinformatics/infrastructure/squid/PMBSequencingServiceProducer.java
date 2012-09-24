package org.broadinstitute.gpinformatics.infrastructure.squid;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.QAInstance;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.QA;
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


    @Produces
    @QAInstance
    public PMBSequencingService produce() {
        return PMBSequencingServiceProducer.qaInstance();
    }


    /**
     * Creates a BSPCohortSearchServiceImpl with plain old new operator for container-free testing,
     * not a managed bean!
     *
     * @return
     */
    public static PMBSequencingService qaInstance() {

        SquidConfig squidConfig = SquidConfigProducer.getConfig(QA);

        return new PMBSequencingServiceImpl( squidConfig );

    }
}
