package org.broadinstitute.sequel.control.quote;


public interface QuoteConnectionParameters {

    public static final String GET_SINGLE_QUOTE_URL = "/portal/Quote/ws/portals/private/getquotes?with_funding=true&quote_alpha_ids=";
    public static final String GET_ALL_SEQUENCING_QUOTES_URL = "/quotes/ws/portals/private/getquotes?platform_name=DNA+Sequencing&with_funding=true";

    String getUrl();

    String getUsername();

    String getPassword();
}
