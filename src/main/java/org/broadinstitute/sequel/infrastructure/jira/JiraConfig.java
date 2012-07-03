package org.broadinstitute.sequel.infrastructure.jira;

import org.broadinstitute.sequel.control.LoginAndPassword;

import java.io.Serializable;

public class JiraConfig implements LoginAndPassword, Serializable {

    private String host;

    private int port;

    private String login;

    private String password;


    public JiraConfig(String host, int port, String login, String password) {
        this.host = host;
        this.port = port;
        this.login = login;
        this.password = password;
    }

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
}
