package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import java.util.List;

/**
 * A lab process, or team, e.g. QTP
 */
public class WorkflowProcessDef {

    private String name;
    /** Process graphs tend to be straight lines, with one or two optional steps (e.g. Normalization is optional in QTP,
     * because the concentration may be fine as is).  (In Squid workflow, new transitions are often initially optional,
     * to allow the automation to be rolled out, but this could be addressed in Mercury by versioning.)
     * Treating steps as lists would simplify visualization and editing. */
    private List<WorkflowStepDef> workflowStepDefs;

    public WorkflowProcessDef(String name) {
        this.name = name;
        this.workflowStepDefs = workflowStepDefs;
    }

    public void addStep(WorkflowStepDef workflowStepDef) {
        workflowStepDefs.add(workflowStepDef);
    }
}
