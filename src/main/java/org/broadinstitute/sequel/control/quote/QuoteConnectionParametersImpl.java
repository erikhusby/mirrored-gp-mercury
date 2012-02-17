package org.broadinstitute.sequel.control.quote;


/**
 * Needs some sort of Maven profile / CDI magic for dealing with config properties
 */

public class QuoteConnectionParametersImpl implements QuoteConnectionParameters {


    private String url      = "https://www.broadinstitute.org/portal/Quote/ws/portals/private/getquotes?quote_alpha_ids=";
    private String username = "rnordin@broadinstitute.org";
    private String password = "Squ1d_us3r";


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
