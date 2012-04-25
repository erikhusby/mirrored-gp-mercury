package org.broadinstitute.sequel.entity.workflow;

/**
 * Represents a transition from one WorkflowState to another, caused by receipt of a message.
 */
public class WorkflowTransition {
    /** Name of the message that causes the transition */
    private String eventTypeName;
    /** State before transition */
    private WorkflowState fromState;
    /** State after transition */
    private WorkflowState toState;

    public WorkflowTransition(String eventTypeName, WorkflowState fromState, WorkflowState toState) {
        this.eventTypeName = eventTypeName;
        this.fromState = fromState;
        this.toState = toState;
    }

    public String getEventTypeName() {
        return this.eventTypeName;
    }

    public WorkflowState getFromState() {
        return this.fromState;
    }

    public WorkflowState getToState() {
        return this.toState;
    }
}
