package org.broadinstitute.pmbridge.infrastructure.quote;


import org.broadinstitute.pmbridge.control.UsernameAndPassword;

public interface QuoteConnectionParameters extends UsernameAndPassword {

    public static final String GET_SINGLE_QUOTE_URL = "/portal/Quote/ws/portals/private/getquotes?with_funding=true&quote_alpha_ids=";
    public static final String GET_ALL_QUOTES_URL = "/quotes/ws/portals/private/getquotes?with_funding=true";
    public static final String GET_ALL_BSP_QUOTES_URL = "/quotes/ws/portals/private/getquotes?platform_name=Biological+Samples&with_funding=true";
    public static final String GET_ALL_GAP_QUOTES_URL = "/quotes/ws/portals/private/getquotes?platform_name=Genetic+Analysis&with_funding=true";
    public static final String GET_ALL_SEQUENCING_QUOTES_URL = "/quotes/ws/portals/private/getquotes?platform_name=DNA+Sequencing&with_funding=true";

    String getUrl(String path);

    String getUsername();

    String getPassword();
}
