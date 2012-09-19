package org.broadinstitute.gpinformatics.athena.infrastructure.quote;


import org.broadinstitute.gpinformatics.athena.control.UsernameAndPassword;

public interface QuoteConnectionParameters extends UsernameAndPassword {


    static final String ROOT_QUOTES_URL = "/quotes/ws/portals/private";
    public static final String GET_QUOTES_BASE_URL = ROOT_QUOTES_URL + "/getquotes?with_funding=true" ;
    public static final String GET_SINGLE_ALPHA_QUOTE_URL = GET_QUOTES_BASE_URL + "&quote_alpha_ids=";
    public static final String GET_SINGLE_NUMERIC_QUOTE_URL = GET_QUOTES_BASE_URL + "&quote_ids=";

    static final String GET_QUOTES_BY_PLATFORM_PARTIAL_URL = GET_QUOTES_BASE_URL + "&platform_name=" ;
    public static final String GET_ALL_BSP_QUOTES_URL = GET_QUOTES_BY_PLATFORM_PARTIAL_URL + QuotePlatformType.BSP.getPlatformName();
    public static final String GET_ALL_SEQ_QUOTES_URL = GET_QUOTES_BY_PLATFORM_PARTIAL_URL + QuotePlatformType.SEQ.getPlatformName();
    public static final String GET_ALL_GAP_QUOTES_URL = GET_QUOTES_BY_PLATFORM_PARTIAL_URL + QuotePlatformType.GAP.getPlatformName();

    public static final String GET_ALL_PRICE_ITEMS = ROOT_QUOTES_URL + "/get_price_list";
    public static final String REGISTER_WORK = ROOT_QUOTES_URL + "/createworkitem";

    String getUrl(String path);

    String getUsername();

    String getPassword();
}
