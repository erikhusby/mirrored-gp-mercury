package org.broadinstitute.sequel.infrastructure.squid;


import org.broadinstitute.sequel.infrastructure.deployment.BaseConfiguration;

public class SquidConfiguration extends BaseConfiguration {


    private String baseUrl;


    public SquidConfiguration(String baseUrl) {
        this.baseUrl = baseUrl;
    }


    public String getBaseUrl() {
        return baseUrl;
    }


}
