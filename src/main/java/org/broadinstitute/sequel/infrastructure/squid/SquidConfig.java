package org.broadinstitute.sequel.infrastructure.squid;


/**
 * Configuration to look up Squid connection parameters, currently limited to the base URL.
 *
 */
public class SquidConfig {


    private String baseUrl;



    public SquidConfig(String baseUrl) {

        this.baseUrl = baseUrl;

    }



    public String getBaseUrl() {
        return baseUrl;
    }

}
