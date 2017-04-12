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

    private String login;
    private String password;

    private String sapShortCloseRecipientEmail;

    private String sapShortCloseEmailSubject;

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
    public String getSapShortCloseRecipientEmail() {
        return sapShortCloseRecipientEmail;
    }

    public void setSapShortCloseRecipientEmail(String sapShortCloseRecipientEmail) {
        this.sapShortCloseRecipientEmail = sapShortCloseRecipientEmail;
    }

    public String getSapShortCloseEmailSubject() {
        return sapShortCloseEmailSubject;
    }

    public void setSapShortCloseEmailSubject(String sapShortCloseEmailSubject) {
        this.sapShortCloseEmailSubject = sapShortCloseEmailSubject;
    }
}
