package org.broadinstitute.sequel.infrastructure.bsp;

import org.broadinstitute.sequel.control.LoginAndPassword;

public class BSPConfig implements LoginAndPassword {


    private String login;

    private String password;

    private String host;

    private int port;

    public BSPConfig(String login, String password, String host, int port) {
        this.login = login;
        this.password = password;
        this.host = host;
        this.port = port;
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
}
