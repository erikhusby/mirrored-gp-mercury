package org.broadinstitute.sequel.entity.queue;

import org.broadinstitute.sequel.entity.labevent.GenericLabEvent;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.labevent.SimpleUserEvent;
import org.broadinstitute.sequel.entity.person.Person;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.workflow.WorkflowDescription;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

public class WorkQueueEntry {

    private Collection<GenericLabEvent> workStartedEvents = new HashSet<GenericLabEvent>();
    
    public WorkQueueEntry(LabVessel vessel,
                          LabWorkQueueParameters workflowParameters,
                          ProjectPlan workflowDescription) {

    }
    
    public void addWorkStarted(Person user) {
        workStartedEvents.add(new SimpleUserEvent(user, LabEventName.WORK_STARTED));
    }
}
