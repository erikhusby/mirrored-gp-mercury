package org.broadinstitute.sequel.infrastructure.quote;


import org.broadinstitute.sequel.control.LoginAndPassword;

public interface QuoteConnectionParameters extends LoginAndPassword {

    public static final String GET_SINGLE_QUOTE_URL = "/quotes/ws/portals/private/getquotes?with_funding=true&quote_alpha_ids=";
    public static final String GET_ALL_SEQUENCING_QUOTES_URL = "/quotes/ws/portals/private/getquotes?platform_name=DNA+Sequencing&with_funding=true";
    public static final String GET_ALL_PRICE_ITEMS = "/quotes/ws/portals/private/get_price_list";
    public static final String REGISTER_WORK = "/quotes/ws/portals/private/createworkitem";

    
    String getUrl(String path);

    String getLogin();

    String getPassword();
}
