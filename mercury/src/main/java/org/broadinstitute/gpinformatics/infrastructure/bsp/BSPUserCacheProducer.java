package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.deployment.TestInstance;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;

public class BSPUserCacheProducer {
    @Produces
    @TestInstance
    @ApplicationScoped
    public BSPUserCache produce(@New BSPUserCache impl) {
        return impl;
    }
}
