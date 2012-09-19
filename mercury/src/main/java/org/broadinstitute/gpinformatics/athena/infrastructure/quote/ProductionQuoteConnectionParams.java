package org.broadinstitute.gpinformatics.athena.infrastructure.quote;


import javax.enterprise.inject.Alternative;

/**
 * Need to figure out how to use CDI/weld to inject this properly.
 */
@Alternative
public class ProductionQuoteConnectionParams implements QuoteConnectionParameters {


    private final String PRODUCTION_HOST = "https://broadinstitute.org";

    private final String username = "athena";
    private final String password = "bspbsp";

    public ProductionQuoteConnectionParams() { }
    
    public String getUrl(String path) {
        return PRODUCTION_HOST + path;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }


}
