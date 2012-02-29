package org.broadinstitute.sequel.control.quote;


/**
 * Needs some sort of Maven profile / CDI magic for dealing with config properties
 */

public class QuoteConnectionParametersImpl implements QuoteConnectionParameters {


    public static final String QA_HOST = "http://quoteqa:8080";
    public static final String PRODUCTION_HOST = "https://broadinstitute.org";
    public static final String GET_SINGLE_QUOTE_URL = "/portal/Quote/ws/portals/private/getquotes?with_funding=true&quote_alpha_ids=";
    public static final String GET_ALL_SEQUENCING_QUOTES_URL = "/quotes/ws/portals/private/getquotes?platform_name=DNA+Sequencing&with_funding=true";
    
    private final String url;
    private final String username = "rnordin@broadinstitute.org";
    private final String password = "Squ1d_us3r";


    public QuoteConnectionParametersImpl(String url) {
        if (url == null) {
            throw new NullPointerException("URL cannot be null.");
        }
        this.url = url;
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
