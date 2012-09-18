package org.broadinstitute.gpinformatics.mercury.infrastructure.jira;

import org.broadinstitute.gpinformatics.mercury.control.LoginAndPassword;
import org.broadinstitute.gpinformatics.mercury.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.mercury.infrastructure.deployment.ConfigKey;

import java.io.Serializable;

@ConfigKey("jira")
public class JiraConfig extends AbstractConfig implements LoginAndPassword, Serializable {

    private String host;

    private int port;

    private String login;

    private String password;


    public JiraConfig() {}


    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
