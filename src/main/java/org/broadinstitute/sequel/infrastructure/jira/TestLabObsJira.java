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

    /**
     *
     *
     * http://vsquid00.broadinstitute.org:8020/rest/api/2/issue/createmeta?projectKeys=LCSET&issuetypeNames=Whole%20Exome%20%28HybSel%29&expand=projects.issuetypes.fields
     * @return
     */

    @Override
    public String getUsername() {
        return "squid";
    }

    @Override
    public String getPassword() {
        return "squid";
    }
}
