package org.broadinstitute.sequel.entity.workflow;

import java.util.Collection;

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

    /**
     * Gets all the annotations that have been added
     * to this transition
     * @return
     */
    public Collection<WorkflowAnnotation> getWorkflowAnnotations() {
        throw new RuntimeException("I haven't been written yet.");
    }
}
