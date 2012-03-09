package org.broadinstitute.sequel.control.jira;


import javax.enterprise.inject.Default;

@Default
public class EriksDesktopJiraConnectionParameters implements JiraConnectionParameters {

    private String username = "mcovarr";

    private String password = "changeme";
    
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
