package org.broadinstitute.sequel.entity.queue;

import org.broadinstitute.sequel.entity.labevent.GenericLabEvent;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.labevent.SimpleUserEvent;
import org.broadinstitute.sequel.entity.person.Person;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.project.SequencingPlanDetail;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.workflow.WorkflowDescription;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

public class WorkQueueEntry {

    private Collection<SimpleUserEvent> workStartedEvents = new HashSet<SimpleUserEvent>();
    
    private LabVessel vessel;
    
    private LabWorkQueueParameters parameters;
    
    private SequencingPlanDetail sequencingPlan;
    
    public WorkQueueEntry(LabVessel vessel,
                          LabWorkQueueParameters workflowParameters,
                          SequencingPlanDetail sequencingPlan) {
        if (vessel == null) {
             throw new NullPointerException("vessel cannot be null."); 
        }
        if (sequencingPlan == null) {
             throw new NullPointerException("sequuencingPlan cannot be null.");
        }
        
        this.vessel = vessel;
        this.sequencingPlan = sequencingPlan;
        this.parameters = workflowParameters;
    }

    public SequencingPlanDetail getSequencingPlan() {
        return sequencingPlan;
    }

    public LabVessel getLabVessel() {
        return vessel;
    }

    public LabWorkQueueParameters getWorkflowParameters() {
        return parameters;
    }

    public void addWorkStarted(Person user) {
        workStartedEvents.add(new SimpleUserEvent(user, LabEventName.WORK_STARTED));
    }
}
