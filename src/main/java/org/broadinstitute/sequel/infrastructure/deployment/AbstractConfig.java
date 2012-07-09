package org.broadinstitute.sequel.infrastructure.deployment;

public abstract class AbstractConfig {

    private Deployment externalDeployment;


    public Deployment getExternalDeployment() {
        return externalDeployment;
    }


    public void setExternalDeployment(Deployment externalDeployment) {
        this.externalDeployment = externalDeployment;
    }


}
