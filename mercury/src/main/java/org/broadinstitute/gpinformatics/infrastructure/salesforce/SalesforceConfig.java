package org.broadinstitute.gpinformatics.infrastructure.salesforce;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.mercury.control.LoginAndPassword;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.Serializable;

/**
 * TODO scottmat fill in javadoc!!!
 */
@SuppressWarnings("UnusedDeclaration")
@ConfigKey("salesforce")
public class SalesforceConfig extends AbstractConfig implements LoginAndPassword, Serializable {

    private String login;

    private String password;

    private String baseUrl;

    private String clientId;

    private String secret;

    private String redirectUrl;

    @Inject
    public SalesforceConfig(@Nonnull Deployment deployment) {
        super(deployment);
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

    public String getClientId() {
        return clientId;
    }

    public String getSecret() {
        return secret;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiUrl(String apiUrl) {
        return apiUrl + "/services/data/v25.0";
    }

    public String getLoginUrl() {
        return getBaseUrl() + "/services/oauth2/token";
    }

    public static SalesforceConfig produce(Deployment deployment) { return produce(SalesforceConfig.class, deployment);}
}
