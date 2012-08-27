package org.broadinstitute.sequel.entity.workflow;

import org.broadinstitute.sequel.entity.labevent.LabEventType;

import java.util.List;

/**
 * A step in a process
 */
public class WorkflowStep {
    private String name;
    private List<LabEventType> labEventTypes;
}
