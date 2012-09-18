package org.broadinstitute.gpinformatics.mercury.infrastructure.deployment;


/**
 * Base class of concrete configs, records the external deployment to which this config corresponds.
 */
public abstract class AbstractConfig {

    private Deployment externalDeployment;


    public Deployment getExternalDeployment() {
        return externalDeployment;
    }


    public void setExternalDeployment(Deployment externalDeployment) {
        this.externalDeployment = externalDeployment;
    }


}
