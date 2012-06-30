package org.broadinstitute.sequel.infrastructure.pmbridge;


public class PMBridgeConnectionParameters {


    private String baseUrl;


    public PMBridgeConnectionParameters(String baseUrl) {
        this.baseUrl = baseUrl;
    }


    public String getBaseUrl() {
        return baseUrl;
    }

}
