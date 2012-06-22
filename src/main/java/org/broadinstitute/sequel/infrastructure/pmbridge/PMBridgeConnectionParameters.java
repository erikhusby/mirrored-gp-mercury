package org.broadinstitute.sequel.infrastructure.pmbridge;


import org.broadinstitute.sequel.infrastructure.deployment.BaseConfiguration;
import org.broadinstitute.sequel.infrastructure.deployment.Deployment;

public class PMBridgeConnectionParameters extends BaseConfiguration {


    private String baseUrl;


    public PMBridgeConnectionParameters(Deployment deployment, String baseUrl) {
        super(deployment);
        this.baseUrl = baseUrl;
    }


    public String getBaseUrl() {
        return baseUrl;
    }

}
