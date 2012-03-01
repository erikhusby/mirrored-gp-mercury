package org.broadinstitute.sequel.control.quote;


/**
 * Need to figure out how to use CDI/weld to inject this properly.
 */
public class ProductionQuoteConnectionParams implements QuoteConnectionParameters {


    private final String PRODUCTION_HOST = "https://broadinstitute.org";

    private final String url;
    private final String username = "rnordin@broadinstitute.org";
    private final String password = "Squ1d_us3r";


    public ProductionQuoteConnectionParams(String path) {
        if (path == null) {
            throw new NullPointerException("URL cannot be null.");
        }
        this.url = PRODUCTION_HOST + path;
    }
    
    public String getUrl() {
        return url;
    }


    public String getUsername() {
        return username;
    }


    public String getPassword() {
        return password;
    }


}
