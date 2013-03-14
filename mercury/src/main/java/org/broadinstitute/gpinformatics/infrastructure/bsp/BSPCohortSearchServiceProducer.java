package org.broadinstitute.gpinformatics.infrastructure.bsp;


import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.QA;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.STUBBY;

public class BSPCohortSearchServiceProducer {

    @Inject
    private Deployment deployment;

    @Produces
    @Default
    @ApplicationScoped
    public BSPCohortSearchService produce(@New BSPCohortSearchServiceStub stub, @New BSPCohortSearchServiceImpl impl) {

        if (deployment == STUBBY) {
            return stub;
        }

        return impl;
    }

    public static BSPCohortSearchService stubInstance() {
        return new BSPCohortSearchServiceStub();
    }


    /**
     * Creates a BSPCohortSearchService with plain old new operator for container-free testing,
     * not a managed bean!
     *
     * @return
     */
    public static BSPCohortSearchService qaInstance() {

        BSPConfig bspConfig = BSPConfig.produce(QA);

        return new BSPCohortSearchServiceImpl(bspConfig);
    }
}
