package org.broadinstitute.gpinformatics.infrastructure.portal;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.mercury.control.LoginAndPassword;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;

/**
 * The YAML config associated object that gets populated from the YAML files.
 */
@SuppressWarnings("UnusedDeclaration")
@ConfigKey("portal")
@ApplicationScoped
public class PortalConfig extends AbstractConfig  implements LoginAndPassword, Serializable  {

    public static final String CRSP_PORTAL_NAME = "CRSP";

    private String login;

    private String password;

    private String host;

    private int port;

    public PortalConfig(){}

    @Inject@Nonnull public PortalConfig(Deployment deployment) {
        super(deployment);
    }

    public String getUrlBase() {
        return String.format("%s%s:%d/portal",getHttpScheme(), getHost(), getPort());
    }

    public String getWsUrl(String portalName) {
        return String.format("%s%s:%d/portal/%s/ws/portals/private/",getHttpScheme(), getHost(), getPort(), portalName);
    }


    public static PortalConfig produce(Deployment deployment) {
        return produce(PortalConfig.class, deployment);
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
