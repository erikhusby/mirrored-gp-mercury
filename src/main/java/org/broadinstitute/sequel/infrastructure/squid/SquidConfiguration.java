package org.broadinstitute.sequel.infrastructure.squid;


import org.broadinstitute.sequel.infrastructure.deployment.BaseConfiguration;
import org.broadinstitute.sequel.infrastructure.deployment.Deployment;

public class SquidConfiguration extends BaseConfiguration {


    private String baseUrl;



    public SquidConfiguration(Deployment deployment, String baseUrl) {

        super(deployment);

        this.baseUrl = baseUrl;

    }



    public String getBaseUrl() {
        return baseUrl;
    }

}
