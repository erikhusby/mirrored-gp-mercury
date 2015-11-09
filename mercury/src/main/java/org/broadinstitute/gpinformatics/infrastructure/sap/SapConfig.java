package org.broadinstitute.gpinformatics.infrastructure.sap;

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
@ConfigKey("sap")
public class SapConfig extends AbstractConfig implements LoginAndPassword, Serializable {

    private String baseUrl;
    private String wsdlUri;

    private String login;
    private String password;

    @Inject
    protected SapConfig(@Nonnull Deployment mercuryDeployment) {
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

    public String getWsdlPath() {
        return baseUrl + wsdlUri;
    }
}
