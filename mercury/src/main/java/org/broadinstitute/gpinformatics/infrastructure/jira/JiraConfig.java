package org.broadinstitute.gpinformatics.infrastructure.jira;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.mercury.control.LoginAndPassword;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import java.io.Serializable;

@ConfigKey("jira")
public class JiraConfig extends AbstractConfig implements LoginAndPassword, Serializable {

    private String host;

    private int port;

    private String login;

    private String password;

    private String urlBase;


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

    public String getUrlBase() {
        return urlBase;
    }

    public void setUrlBase(String urlBaseIn) {
        urlBase = urlBaseIn;
    }

    public String createTicketUrl(String jiraTicketName) {
        return getUrlBase() + "/browse/" + jiraTicketName;
    }

    @Produces
    @Default
    public JiraConfig produce() {
        return produce(JiraConfig.class);
    }


    public static JiraConfig produce(Deployment deployment) {
        return produce(JiraConfig.class, deployment);
    }
}
