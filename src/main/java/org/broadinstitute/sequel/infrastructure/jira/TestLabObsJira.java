package org.broadinstitute.sequel.infrastructure.jira;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;

@Default
public class TestLabObsJira implements JiraConnectionParameters {

    public TestLabObsJira() {}

    @Override
    public int getPort() {
        return 8020;
    }

    @Override
    public String getHostname() {
        return "vsquid00.broadinstitute.org";
    }

    @Override
    public String getUsername() {
        return "squid";
    }

    @Override
    public String getPassword() {
        return "squid";
    }
}
