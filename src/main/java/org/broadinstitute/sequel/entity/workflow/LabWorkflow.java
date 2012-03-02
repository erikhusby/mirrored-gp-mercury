package org.broadinstitute.sequel.entity.workflow;

/**
 * High level lab protocol which corresponds
 * to a menu item in GSP's pricing list
 * and also to a labops jira workflow type.
 *
 * For example: HybridSelection 7.0.
 */
public class LabWorkflow {

    private String workflowName;
    
    private String version;
    
    public LabWorkflow(String workflowName,String version) {
        if (workflowName == null) {
             throw new NullPointerException("workflowName cannot be null."); 
        }
        this.workflowName = workflowName;
        this.version = version;
    }
    
    /**
     * Name of the workflow.
     * @return
     */
    public String getName() {
        return workflowName;
    }
    
    /**
     * Optional version.  Early in a project,
     * the version might not be set.  By the
     * time the project starts, we might
     * just take whatever the latest stable
     * production version is, so null here
     * might be interpreted as "latest".
     * @return
     */
    public String getVersion() {
        return version;
    }

}
