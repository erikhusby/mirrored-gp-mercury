package org.broadinstitute.sequel.entity.queue;

import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.person.Person;
import org.broadinstitute.sequel.entity.project.BasicProjectPlan;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;
import org.broadinstitute.sequel.entity.vessel.LabVessel;

import java.util.Collection;
import java.util.HashSet;

public class WorkQueueEntry<T extends LabWorkQueueParameters> {

    private LabVessel vessel;
    
    private T parameters;

    private WorkflowDescription workflowDescription;

    private LabWorkQueue queue;

    private BasicProjectPlan projectPlanOverride;

    public WorkQueueEntry(LabWorkQueue queue,
                          LabVessel vessel,
                          T workflowParameters,
                          WorkflowDescription workflowDescription,
                          BasicProjectPlan projectPlanOverride) {
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
        this.parameters = workflowParameters;
        this.workflowDescription = workflowDescription;
        this.projectPlanOverride = projectPlanOverride;
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


    public BasicProjectPlan getProjectPlanOverride() {
        return projectPlanOverride;
    }
}
