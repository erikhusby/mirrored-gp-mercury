package org.broadinstitute.sequel.infrastructure.quote;


import org.broadinstitute.sequel.control.LoginAndPassword;

import java.io.Serializable;

public class QuoteConfig implements LoginAndPassword, Serializable {

    private String login;

    private String password;

    private String baseUrl;


    public QuoteConfig( String login, String password, String baseUrl ) {

        this.login    = login;
        this.password = password;
        this.baseUrl  = baseUrl;
    }

    @Override
    public String getLogin() {
        return login;
    }


    @Override
    public String getPassword() {
        return password;
    }


    public String getBaseUrl() {
        return baseUrl;
    }


}
