package org.broadinstitute.sequel.infrastructure.quote;


import org.broadinstitute.sequel.control.LoginAndPassword;
import org.broadinstitute.sequel.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.sequel.infrastructure.deployment.ConfigKey;

import java.io.Serializable;

@ConfigKey("quote")
public class QuoteConfig extends AbstractConfig implements LoginAndPassword, Serializable {

    private String login;

    private String password;

    private String url;


    public QuoteConfig() {
    }


    @Override
    public String getLogin() {
        return login;
    }


    @Override
    public String getPassword() {
        return password;
    }


    public String getUrl() {
        return url;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
