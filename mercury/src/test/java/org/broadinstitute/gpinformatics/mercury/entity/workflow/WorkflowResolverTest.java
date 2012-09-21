package org.broadinstitute.gpinformatics.mercury.entity.workflow;


import org.broadinstitute.gpinformatics.mercury.entity.project.WorkflowDescription;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.TestGroups.DATABASE_FREE;

import java.util.Collection;

import static org.testng.Assert.*;

public class WorkflowResolverTest {

    @Test(groups = {DATABASE_FREE})
    public void test_two_workflows() {
        Collection<WorkflowDescription> workflows = new WorkflowResolver().getActiveWorkflows("Middle");
        assertEquals(workflows.size(), 2);
    }

    @Test(groups = {DATABASE_FREE})
    public void test_single_workflow() {
        Collection<WorkflowDescription> workflows = new WorkflowResolver().getActiveWorkflows("More");
        assertEquals(workflows.size(), 1);
        assertEquals(workflows.iterator().next().getWorkflowName(),"TestWorkflow2");
    }
}
