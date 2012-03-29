package org.broadinstitute.sequel.infrastructure.quote;

import javax.enterprise.inject.Default;

/**
 * Need to figure out how to get CDI to configure
 * this for our integration test.
 */
@Default
public class QAQuoteConnectionParams implements QuoteConnectionParameters {

    private final String QA_HOST = "http://quoteqa:8080";

     private final String username = "rnordin@broadinstitute.org";
    private final String password = "Squ1d_us3r";

    public QAQuoteConnectionParams() {}

    public String getUrl(String path) {
        return QA_HOST + path;
    }


    public String getUsername() {
        return username;
    }


    public String getPassword() {
        return password;
    }


}
