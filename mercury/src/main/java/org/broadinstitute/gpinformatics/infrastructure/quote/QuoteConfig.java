package org.broadinstitute.gpinformatics.infrastructure.quote;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.mercury.control.LoginAndPassword;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
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

    @Produces
    @Default
    public QuoteConfig produce() {
        return produce(QuoteConfig.class);
    }

    public static QuoteConfig produce(Deployment deployment) {
        return produce(QuoteConfig.class, deployment);
    }
}