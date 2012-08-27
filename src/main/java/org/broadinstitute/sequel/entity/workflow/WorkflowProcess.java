package org.broadinstitute.sequel.entity.workflow;

import java.util.List;

/**
 * A lab process, or team, e.g. QTP
 */
public class WorkflowProcess {
    private String name;
    private List<WorkflowStep> workflowSteps;
}
