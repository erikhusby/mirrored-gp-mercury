package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.mercury.control.LoginAndPassword;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.Serializable;


@SuppressWarnings("UnusedDeclaration")
@ConfigKey("bsp")
public class BSPConfig extends AbstractConfig implements LoginAndPassword, Serializable {

    private String login;

    private String password;

    private String host;

    private int port;

    @Inject
    public BSPConfig(@Nonnull Deployment deployment) {
        super(deployment);
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

    public String getUrl(String suffix) {
        return String.format("http://%s:%d/BSP/%s", getHost(), getPort(), suffix);
    }

    public String getWSUrl(String suffix) {
        return String.format("http://%s:%d/ws/bsp/%s", getHost(), getPort(), suffix);
    }

    public static BSPConfig produce(Deployment deployment) {
        return produce(BSPConfig.class, deployment);
    }

}
