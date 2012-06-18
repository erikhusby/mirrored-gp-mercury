package org.broadinstitute.sequel.infrastructure.jira;

public class TestLabObsJira implements JiraConnectionParameters {

    @Override
    public int getPort() {
        return 8080;
    }

    @Override
    public String getHostname() {
        return "labopsjiradev.broadinstitute.org:8080";
    }

    @Override
    public String getUsername() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public String getPassword() {
        throw new RuntimeException("I haven't been written yet.");
    }
}
