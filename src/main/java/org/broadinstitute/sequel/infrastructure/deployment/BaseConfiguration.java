package org.broadinstitute.sequel.infrastructure.deployment;


/**
 * Base class for configurations, recording the {@link Deployment} associated with the configuration.
 *
 */
public class BaseConfiguration {

    private Deployment deployment;

    public BaseConfiguration(Deployment deployment) {
        this.deployment = deployment;
    }


    public Deployment getDeployment() {
        return deployment;
    }

}
