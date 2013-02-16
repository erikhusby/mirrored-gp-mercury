package org.broadinstitute.gpinformatics.mercury.entity.workflow;

/**
* @author Scott Matthews
*         Date: 2/15/13
*         Time: 4:03 PM
*/
public enum WorkflowName {
    EXOME_EXPRESS("Exome Express"),
    HYBRID_SELECTION("Hybrid Selection"),
    WHOLE_GENOME("Whole Genome");

    private final String workflowName;

    WorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public WorkflowName findByName(String searchName) {

        WorkflowName foundName = null;

        for (WorkflowName currName:values()) {
            if(currName.getWorkflowName().equals(searchName)) {
                foundName = currName;
            }
        }

        return foundName;
    }
}
