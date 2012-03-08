package org.broadinstitute.sequel.entity.workflow;

public class WorkflowState {
    
    private String state;
    
    public WorkflowState(String state) {
        if (state == null) {
             throw new NullPointerException("state cannot be null."); 
        }
        this.state = state;
    }
    
    public String getState() {
        return state;
    }
}
