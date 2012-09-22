package org.broadinstitute.gpinformatics.infrastructure.gap;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.QAInstance;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.QA;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.STUBBY;

public class GenotypingServiceProducer {

    @Inject
    private Deployment deployment;


    @Produces
    @Default
    @SessionScoped
    public GenotypingService produce(@New GenotypingServiceImpl impl) {

        // no mock impl defined
        if (deployment == STUBBY)
            return null;

        return impl;
    }


    @Produces
    @QAInstance
    public GenotypingService produce() {
        return GenotypingServiceProducer.qaInstance();
    }


    /**
     * Creates a PMBQuoteServiceImpl with plain old new operator for container-free testing,
     * not a managed bean!
     *
     * @return
     */
    public static GenotypingService qaInstance() {

        GAPConfig gapConfig = GAPConfigProducer.getConfig(QA);

        return new GenotypingServiceImpl( gapConfig );

    }

}
