package org.broadinstitute.sequel.entity.workflow;

import org.broadinstitute.sequel.entity.labevent.LabEventType;

import java.util.List;

/**
 * A step in a process
 */
public class WorkflowStep {
    private String name;
    private List<LabEventType> labEventTypes;
    // checkpoint - decision
    // entry point - support adding sample metadata for walk up sequencing
    // re-entry point - notice to batch watchers
    // QC point - data uploaded

    public WorkflowStep(String name) {
        this.name = name;
    }

    public WorkflowStep addLabEvent(LabEventType labEventType) {
        labEventTypes.add(labEventType);
        return this;
    }
}
