package org.broadinstitute.sequel.infrastructure.squid;


import org.broadinstitute.sequel.infrastructure.deployment.BaseConfiguration;
import org.broadinstitute.sequel.infrastructure.deployment.Deployment;


/**
 * Configuration to look up Squid connection parameters, currently limited to the base URL.
 *
 */
public class SquidConnectionParameters extends BaseConfiguration {


    private String baseUrl;



    public SquidConnectionParameters(Deployment deployment, String baseUrl) {

        super(deployment);

        this.baseUrl = baseUrl;

    }



    public String getBaseUrl() {
        return baseUrl;
    }

}
