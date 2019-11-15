package org.broadinstitute.gpinformatics.infrastructure.presentation;

import javax.enterprise.context.Dependent;

@Dependent
public class DashboardLink {
    public String runStatusPage(String flowcell) {
        return "https://illuminadashboard.broadinstitute.org:8181/?runFolder=" + flowcell;
    }
}
