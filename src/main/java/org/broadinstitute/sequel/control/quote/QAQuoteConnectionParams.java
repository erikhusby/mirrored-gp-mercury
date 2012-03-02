package org.broadinstitute.sequel.control.quote;

/**
 * Need to figure out how to get CDI to configure
 * this for our integration test.
 */
public class QAQuoteConnectionParams implements QuoteConnectionParameters {

    private final String QA_HOST = "http://quoteqa:8080";

    private final String url;
    private final String username = "rnordin@broadinstitute.org";
    private final String password = "Squ1d_us3r";


    public QAQuoteConnectionParams(String path) {
        url = QA_HOST + path;
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
