package org.broadinstitute.sequel.entity.project;

import org.testng.annotations.Test;

public class ProjectTest {
    
    @Test(enabled = false)
    public void test_legacy_squid_project() {
        // project per squid project
        Project legacyProject = new BaseProject("Legacy Squid Project C203");

        // plan per work request
        ProjectPlan plan = new ProjectPlan(legacyProject,legacyProject.getProjectName() + " Plan");

        // one sequencing detail per plan
        legacyProject.addProjectPlan(plan);

        // starters are GSSRSample, just an ID.
        // or SquidLibraries, just an ID.
        
    }
}
