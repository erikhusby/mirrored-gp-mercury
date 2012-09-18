package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.project.WorkflowDescription;

import java.util.HashSet;
import java.util.Set;

public class WorkflowState {
    
    private String state;
    private WorkflowDescription workflowDescription;
    private Set<WorkflowTransition> entries = new HashSet<WorkflowTransition>();
    private Set<WorkflowTransition> exits = new HashSet<WorkflowTransition>();

    public WorkflowState(String state) {
        if (state == null) {
             throw new NullPointerException("state cannot be null."); 
        }
        this.state = state;
    }
    
    public String getState() {
        return this.state;
    }

    public WorkflowDescription getWorkflowDescription() {
        return this.workflowDescription;
    }

    public void setWorkflowDescription(WorkflowDescription workflowDescription) {
        this.workflowDescription = workflowDescription;
    }

    public Set<WorkflowTransition> getEntries() {
        return this.entries;
    }

    // for JPA
    private void setEntries(Set<WorkflowTransition> entries) {
        this.entries = entries;
    }

    public Set<WorkflowTransition> getExits() {
        return this.exits;
    }

    // for JPA
    private void setExits(Set<WorkflowTransition> exits) {
        this.exits = exits;
    }
}
