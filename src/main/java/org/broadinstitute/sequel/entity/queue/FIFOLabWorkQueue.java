package org.broadinstitute.sequel.entity.queue;


import org.broadinstitute.sequel.entity.person.Person;
import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.project.SequencingPlanDetail;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.MolecularStateRange;
import org.broadinstitute.sequel.entity.workflow.WorkflowDescription;

import java.util.*;

/**
 * Add things in and they come out in fifo
 * order.  Probably most queues in the lab
 * will work this way initially.
 */
public class FIFOLabWorkQueue<T extends LabWorkQueueParameters> implements FullAccessLabWorkQueue<T> {

    // order matters: fifo
    private List<WorkQueueEntry> requestedWork = new ArrayList<WorkQueueEntry>();

    private LabWorkQueueName name;
    
    public FIFOLabWorkQueue(LabWorkQueueName name) {
        if (name == null) {
             throw new NullPointerException("name cannot be null.");
        }
        this.name = name;
    }

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
                if (workflow.equals(queuedWork.getSequencingPlan().getWorkflow())) {
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
        SequencingPlanDetail sequencingPlan = queuedWork.getSequencingPlan();
        ProjectPlan projectPlan = sequencingPlan.getProjectPlan();
        Project p = projectPlan.getProject();
        JiraTicket ticket = p.getJiraTicket();
        if (ticket != null) {
            ticket.addComment(user.getFirstName() + " " + user.getLastName() + " has started work for plan '" +
                    projectPlan.getName() + "' for starter " + queuedWork.getLabVessel().getLabel());
        }
    }

    @Override
    public LabWorkQueueResponse add(LabVessel vessel, 
                                    T workflowParameters, 
                                    SequencingPlanDetail sequencingDetail) {
        if (vessel == null) {
             throw new NullPointerException("vessel cannot be null.");
        }
        if (sequencingDetail == null) {
             throw new NullPointerException("projectPlan cannot be null.");
        }
        WorkQueueEntry newWork = new WorkQueueEntry(vessel,workflowParameters,sequencingDetail);
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

    @Override
    public boolean isEmpty() {
        return requestedWork.isEmpty();
    }

    @Override
    public Collection<LabVessel> suggestNextBatch(int batchSize, T bucket) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabVessel> peek(T bucket) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabVessel> peekAll() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void moveToTop(LabVessel vessel, T bucket) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void startWork(LabVessel vessel, T bucket) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void markComplete(LabVessel vessel, T bucket) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void printWorkSheet(Collection<LabVessel> vessel, T bucket) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<T> getContainingBuckets(LabVessel vessel) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public int getNumOrbits(LabVessel vessel) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public LabWorkQueueName getQueueName() {
        return name;
    }

    @Override
    public Collection<MolecularStateRange> getMolecularStateRequirements() {
        throw new RuntimeException("I haven't been written yet.");
    }
}
