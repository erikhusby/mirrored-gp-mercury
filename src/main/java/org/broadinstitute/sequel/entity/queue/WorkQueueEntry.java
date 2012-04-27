package org.broadinstitute.sequel.entity.queue;

import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.labevent.SimpleUserEvent;
import org.broadinstitute.sequel.entity.person.Person;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.project.SequencingPlanDetail;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;
import org.broadinstitute.sequel.entity.vessel.LabVessel;

import java.util.Collection;
import java.util.HashSet;

public class WorkQueueEntry<T extends LabWorkQueueParameters> {

    private Collection<SimpleUserEvent> workStartedEvents = new HashSet<SimpleUserEvent>();
    
    private LabVessel vessel;
    
    private T parameters;

    private WorkflowDescription workflowDescription;

    private LabWorkQueue queue;

    public WorkQueueEntry(LabWorkQueue queue,
                          LabVessel vessel,
                          T workflowParameters,
                          WorkflowDescription workflowDescription) {
        if (queue == null) {
            throw new RuntimeException("Queue cannot be null.");
        }
        if (vessel == null) {
             throw new NullPointerException("vessel cannot be null."); 
        }
        if (workflowDescription == null) {
            throw new RuntimeException("workflowDescription cannot be null.");
        }
        this.vessel = vessel;
        this.queue = queue;
        vessel.addWorkQueueEntry(this);
        this.parameters = workflowParameters;
        this.workflowDescription = workflowDescription;
    }

    public void dequeue() {
        queue.remove(this);
    }

    public LabVessel getLabVessel() {
        return vessel;
    }

    public T getLabWorkQueueParameters() {
        return parameters;
    }

    public WorkflowDescription getWorkflowDescription() {
        return workflowDescription;
    }

    public void addWorkStarted(Person user) {
        workStartedEvents.add(new SimpleUserEvent(user, LabEventName.WORK_STARTED));
    }

    public ProjectPlan getProjectPlanOverride() {
        throw new RuntimeException("I haven't been written yet.");
    }
}
