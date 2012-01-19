package org.broadinstitute.sequel;

/**
 * A stub description of the end-to-end
 * workflow.  Things like "Hybrid Selection v8.0"
 * and "WGS 7.2"
 */
public class WorkflowDescription {

    private final String workflowName;

    private final String version;
    
    public WorkflowDescription(String workflowName,String version) {
        if (workflowName == null) {
             throw new IllegalArgumentException("workflowName must be non-null in WorkflowDescription.WorkflowDescription");
        }
        this.workflowName = workflowName;
        this.version = version;
    }    

    public String getWorkflowName() {
        return workflowName;
    }
    
    public String getWorkflowVersion() {
        return version;
    }

}
