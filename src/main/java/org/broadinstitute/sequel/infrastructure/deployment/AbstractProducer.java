package org.broadinstitute.sequel.infrastructure.deployment;


import org.apache.commons.logging.Log;

import javax.inject.Inject;

public abstract class AbstractProducer {

    @Inject
    private Deployment deployment;

    @Inject
    protected Log log;


    public Deployment getDeployment() {
        return deployment;
    }

}
