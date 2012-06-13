package org.broadinstitute.sequel.entity.workflow;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

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

    private final Collection<WorkflowAnnotation> workflowAnnotations = new HashSet<WorkflowAnnotation>();

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

    public void addWorkflowAnnotation(WorkflowAnnotation annotation) {
        workflowAnnotations.add(annotation);
    }

    /**
     * Gets all the annotations that have been added
     * to this transition
     * @return
     */
    public Collection<WorkflowAnnotation> getWorkflowAnnotations() {
        return Collections.unmodifiableCollection(workflowAnnotations);
    }
}
