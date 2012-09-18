package org.broadinstitute.gpinformatics.mercury.infrastructure.bsp;

import org.broadinstitute.gpinformatics.mercury.control.LoginAndPassword;
import org.broadinstitute.gpinformatics.mercury.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.mercury.infrastructure.deployment.ConfigKey;

import java.io.Serializable;


@ConfigKey("bsp")
public class BSPConfig extends AbstractConfig implements LoginAndPassword, Serializable {


    private String login;

    private String password;

    private String host;

    private int port;

    public BSPConfig() {}


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
