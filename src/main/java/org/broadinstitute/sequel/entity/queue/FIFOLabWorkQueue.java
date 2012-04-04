package org.broadinstitute.sequel.entity.queue;


import org.broadinstitute.sequel.entity.sample.PostProjectComment;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.workflow.Workflow;
import org.broadinstitute.sequel.entity.workflow.WorkflowEngine;
import org.broadinstitute.sequel.entity.person.Person;
import org.broadinstitute.sequel.entity.project.*;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.MolecularStateRange;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;
import org.broadinstitute.sequel.entity.workflow.WorkflowState;
import org.broadinstitute.sequel.infrastructure.jira.JiraService;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueResponse;

import java.io.IOException;
import java.util.*;

/**
 * Add things in and they come out in fifo
 * order.  Probably most queues in the lab
 * will work this way initially.  This queue
 * automatically creates a {@link JiraTicket}
 * for every batch of {@link LabVessel}s 
 * when {@link #startWork(java.util.Collection, LabWorkQueueParameters, org.broadinstitute.sequel.entity.project.WorkflowDescription, org.broadinstitute.sequel.entity.person.Person)} is called.
 */
public class FIFOLabWorkQueue<T extends LabWorkQueueParameters> implements FullAccessLabWorkQueue<T> {

    // order matters: fifo
    private List<WorkQueueEntry<T>> requestedWork = new ArrayList<WorkQueueEntry<T>>();

    private LabWorkQueueName name;
    
    private WorkflowEngine workflowEngine;
    
    private JiraService jiraService;
    
    public FIFOLabWorkQueue(LabWorkQueueName name,
                            WorkflowEngine workflowEngine,
                            JiraService jiraService) {
        if (name == null) {
             throw new NullPointerException("name cannot be null.");
        }
        if (workflowEngine == null) {
             throw new NullPointerException("workflowEngine cannot be null."); 
        }
        this.name = name;
        this.workflowEngine = workflowEngine;
        this.jiraService = jiraService;
    }

    @Override
    /**
     * Starts work for all the vessels, and creates a
     * new {@link JiraTicket} for the batch.  The details
     * of the {@link JiraTicket} are controlled at least in
     * part by {@link T the parameters for the queue}.
     */
    public JiraLabWorkQueueResponse startWork(Collection<LabVessel> vessels, T workflowParameters, WorkflowDescription workflow, Person user) {
        for (LabVessel vessel : vessels) {
            startWork(vessel,workflowParameters,workflow,user);
        }

        CreateIssueResponse jiraResponse = createJiraTicket(vessels,workflow,workflowParameters,jiraService);

        JiraTicket ticket = new JiraTicket(jiraService,jiraResponse.getTicketName(),jiraResponse.getId());
        for (LabVessel vessel : vessels) {
            vessel.addJiraTicket(ticket);
        }
        PostProjectComment.postUpdate("Workflow update",user.getLogin() + " has created " + ticket.getTicketName() + " for the following samples:",vessels);

        return new JiraLabWorkQueueResponse("OK",ticket);
    }

    private CreateIssueResponse createJiraTicket(Collection<LabVessel> vessels,
                                                 WorkflowDescription workflowDescription,
                                                 T parameters,
                                                 JiraService jiraService) {
        Collection<Project> allProjects = new HashSet<Project>();
        for (LabVessel vessel : vessels) {
            for (SampleInstance sampleInstance : vessel.getSampleInstances()) {
                for (ProjectPlan projectPlan : sampleInstance.getAllProjectPlans()) {
                    allProjects.add(projectPlan.getProject());
                }
            }
        }

        String ticketTitle = null;
        StringBuilder ticketDetails = new StringBuilder();
        if (allProjects.size() == 1) {
            Project singleProject = allProjects.iterator().next();
            ticketTitle = "Work for " + singleProject.getProjectName();
            ticketDetails.append(singleProject.getProjectName());
        }
        else {
            ticketTitle = "Work for " + allProjects.size() + " projects";
            for (Project project : allProjects) {
                ticketDetails.append(project.getProjectName()).append(" ");
            }
        }

        CreateIssueResponse jiraTicketCreationResponse =  null;

        try {
            jiraTicketCreationResponse = jiraService.createIssue(workflowDescription.getJiraProjectPrefix(),
                    workflowDescription.getJiraIssueType(),
                    ticketTitle,
                    ticketDetails.toString());
            // todo use #lcSetParameters to add more details to the ticket

        }
        catch (IOException e) {
            throw new RuntimeException("Failed to create jira ticket",e);
        }
        return jiraTicketCreationResponse;
    }

    public void startWork(LabVessel vessel, 
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
        for (WorkQueueEntry<T> queuedWork: requestedWork) {
            if (vessel.equals(queuedWork.getLabVessel())) {
                if (workflow.equals(queuedWork.getSequencingPlan().getProjectPlan().getWorkflowDescription())) {
                    if (workflowParameters == null) {
                        if (queuedWork.getLabWorkQueueParameters() == null) {
                            foundIt = true;
                        }
                    }
                    else {
                        if (workflowParameters.equals(queuedWork.getLabWorkQueueParameters())) {
                            foundIt = true;
                        }
                    }                   
                }
            }
            if (foundIt) {
                requestedWork.remove(queuedWork);
                applyWorkflowStateChange(vessel,workflow);
                break;
            }
        }
        if (!foundIt) {
            // todo log this somewhere
        }
    }
    
    private void startWorkflow(LabVessel vessel,ProjectPlan plan,T workflowParameters) {
        Collection<LabVessel> vessels = new ArrayList<LabVessel>(1);
        vessels.add(vessel);
        workflowEngine.addWorkflow(new Workflow(plan,vessels,workflowParameters));
    }
    
    private void applyWorkflowStateChange(LabVessel vessel,
                                          WorkflowDescription workflowDescription) {
        Collection<Workflow> activeWorkflows = workflowEngine.getActiveWorkflows(vessel,workflowDescription);
        
        if (activeWorkflows.isEmpty()) {
            // todo: basic logging and email writing infrastructure
        }
        if (activeWorkflows.size() > 1) {
            // instead of dumping this stuff to a log file that no one
            // reads, we could put it in the face of project managers...
            vessel.addNoteToProjects(vessel.getLabel() + " is mapped to " + activeWorkflows.size() + " active workflows.");
        }
        
        // suppose there are multiple active workflows for this container
        // and workflow description.  That means the system has duplicate
        // work queued up.  If that's the case, then it doesn't matter
        // which one workflow chooses, as long as eventually it chooses
        // both.
        Workflow workflow = activeWorkflows.iterator().next();
        workflow.setState(new WorkflowState("work has stated"));
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
            startWorkflow(vessel,sequencingDetail.getProjectPlan(),workflowParameters);
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

    @Override
    public WorkflowEngine getWorkflowEngine() {
        return workflowEngine;
    }

}
