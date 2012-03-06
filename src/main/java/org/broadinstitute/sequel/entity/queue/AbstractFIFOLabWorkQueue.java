package org.broadinstitute.sequel.entity.queue;


import org.broadinstitute.sequel.entity.person.Person;
import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.workflow.WorkflowDescription;

import java.util.*;

/**
 * Add things in and they come out in fifo
 * order.  Probably most queues in the lab
 * will work this way initially.
 */
public abstract class AbstractFIFOLabWorkQueue<T extends LabWorkQueueParameters> implements FullAccessLabWorkQueue<T> {

    // order matters: fifo
    private List<WorkQueueEntry> requestedWork = new ArrayList<WorkQueueEntry>();
    
    @Override
    public LabWorkQueueResponse startWork(LabVessel vessel, 
                                          T workflowParameters, 
                                          WorkflowDescription workflow,
                                          Person user) {
        if (vessel == null) {
             throw new NullPointerException("vessel cannot be null.");
        }
        if (workflow == null) {
             throw new NullPointerException("workflow cannot be null.");
        }

        // because we allow duplicate entries for duplicate work,
        // and because we expect to see the same {@link LabVessel}
        // queued for work across different {@link Project}s and
        // different {@link ProjectPlan}s, we play pin-the-tail-on-the-ProjectPlan
        // here, in FIFO order.
        boolean foundIt = false;
        for (WorkQueueEntry queuedWork: requestedWork) {
            if (vessel.equals(queuedWork.getLabVessel())) {
                if (workflow.equals(queuedWork.)) {
                    if (workflowParameters == null) {
                        if (queuedWork.getWorkflowParameters() == null) {
                            foundIt = true;
                        }
                    }
                    else {
                        if (workflowParameters.equals(queuedWork.getWorkflowParameters())) {
                            foundIt = true;
                        }
                    }                   
                }
            }
            if (foundIt) {
                requestedWork.remove(queuedWork);
                markWorkStarted(queuedWork,user);
                break;
            }
        }
        if (foundIt) {
            return new StandardLabWorkQueueResponse("OK");
        }
        else {
            return new StandardLabWorkQueueResponse(vessel.getLabel() + " has not been queued for work.  Proceed at your own risk");
        }
    }
    
    private void markWorkStarted(WorkQueueEntry queuedWork,Person user) {
        queuedWork.addWorkStarted(user);
        ProjectPlan projectPlan = queuedWork.getProjectPlan();
        Project p = projectPlan.getProject();
        JiraTicket ticket = p.getJiraTicket();
        if (ticket != null) {
            ticket.addComment(user.getFirstName() + " " + user.getLastName() + " has started work for plan '" +
                    projectPlan.getName() + "' for starter " + queuedWork.getLabVessel().getLabel());
        }
    }

    @Override
    public LabWorkQueueResponse add(LabVessel vessel, T workflowParameters, ProjectPlan projectPlan) {
        if (vessel == null) {
             throw new NullPointerException("vessel cannot be null.");
        }
        if (projectPlan == null) {
             throw new NullPointerException("projectPlan cannot be null.");
        }
        WorkQueueEntry newWork = new WorkQueueEntry(vessel,workflowParameters,projectPlan);
        LabWorkQueueResponse response = null;
        if (requestedWork.contains(newWork)) {
            response = new StandardLabWorkQueueResponse(vessel.getLabel() + " is already in " + getQueueName() + "; duplicate work has been requested."); 
        }
        else {
            response = new StandardLabWorkQueueResponse("Added " + vessel.getLabel() + " to " + getQueueName());
        }
        requestedWork.add(newWork);
        
        return response;
    }


}
