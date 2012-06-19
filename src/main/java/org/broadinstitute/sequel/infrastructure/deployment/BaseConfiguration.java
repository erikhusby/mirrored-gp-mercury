package org.broadinstitute.sequel.infrastructure.deployment;

public class BaseConfiguration {

    private Deployment deployment;

    public BaseConfiguration(Deployment deployment) {
        this.deployment = deployment;
    }


    public Deployment getDeployment() {
        return deployment;
    }

}
