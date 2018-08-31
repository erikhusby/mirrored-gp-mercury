package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.mercury.control.LoginAndPassword;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;

@SuppressWarnings("UnusedDeclaration")
@ConfigKey("sap")
@ApplicationScoped
public class SapConfig extends AbstractConfig implements LoginAndPassword, Serializable {
    private static final String SHORT_CLOSE_REQUEST = "Short Close Request";
    private static final String REVERSE_BILLING_REQUEST = "Reverse Billing Request";

    private String login;
    private String password;

    private String sapSupportEmail;

    private String sapSupportEmailSubjectPrefix;

    public SapConfig(){}

    @Inject
    public SapConfig(@Nonnull Deployment mercuryDeployment) {
        super(mercuryDeployment);
    }

    @Override
    public String getLogin() {
        return login;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static SapConfig produce(Deployment deployment) {
        return produce(SapConfig.class, deployment);
    }
    public String getSapSupportEmail() {
        return sapSupportEmail;
    }

    public void setSapSupportEmail(String sapSupportEmail) {
        this.sapSupportEmail = sapSupportEmail;
    }

    public String getSapShortCloseEmailSubject() {
        return getSapSupportEmailSubjectPrefix() + ": " + SHORT_CLOSE_REQUEST;
    }

    public String getSapReverseBillingSubject() {
        return getSapSupportEmailSubjectPrefix() + ": " + REVERSE_BILLING_REQUEST;
    }

    public String getSapSupportEmailSubjectPrefix() {
        return sapSupportEmailSubjectPrefix;
    }

    public void setSapSupportEmailSubjectPrefix(String sapSupportEmailSubjectPrefix) {
        this.sapSupportEmailSubjectPrefix = sapSupportEmailSubjectPrefix;
    }
}
