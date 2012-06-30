package org.broadinstitute.sequel.infrastructure.pmbridge;


public class PMBridgeConfig {


    private String baseUrl;


    public PMBridgeConfig(String baseUrl) {
        this.baseUrl = baseUrl;
    }


    public String getBaseUrl() {
        return baseUrl;
    }

}
