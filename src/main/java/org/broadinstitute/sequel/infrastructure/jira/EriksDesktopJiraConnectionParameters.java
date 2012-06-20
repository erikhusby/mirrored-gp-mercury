package org.broadinstitute.sequel.infrastructure.jira;


import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;

@Alternative
public class EriksDesktopJiraConnectionParameters implements JiraConnectionParameters {

    private String username = "sequel";

    private String password = "sequel";
    
    private int port = 8080;

    private String hostname = "cp67e-eb3.broadinstitute.org";
    

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getHostname() {
        return hostname;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }
}
