package org.broadinstitute.sequel.infrastructure.pmbridge;


import java.io.Serializable;

public class PMBridgeConfig implements Serializable {


    private String baseUrl;


    public PMBridgeConfig(String baseUrl) {
        this.baseUrl = baseUrl;
    }


    public String getBaseUrl() {
        return baseUrl;
    }

}
