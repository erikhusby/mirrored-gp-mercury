package org.broadinstitute.sequel.infrastructure.squid;


/**
 * Configuration to look up Squid connection parameters, currently limited to the base URL.
 *
 */
public class SquidConnectionParameters {


    private String baseUrl;



    public SquidConnectionParameters(String baseUrl) {

        this.baseUrl = baseUrl;

    }



    public String getBaseUrl() {
        return baseUrl;
    }

}
