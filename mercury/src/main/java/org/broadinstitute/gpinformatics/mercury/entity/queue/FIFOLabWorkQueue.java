package org.broadinstitute.gpinformatics.mercury.entity.queue;


import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.gpinformatics.mercury.entity.sample.JiraCommentUtil;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.broadinstitute.gpinformatics.mercury.entity.project.*;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MolecularStateRange;
import org.broadinstitute.gpinformatics.mercury.entity.project.WorkflowDescription;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueResponse;

import java.io.IOException;
import java.util.*;

/**
 * Add things in and they come out in fifo
 * order.  Probably most queues in the lab
 * will work this way initially.  This queue
 * automatically creates a {@link JiraTicket}
 * for every batch of {@link LabVessel}s 
 * when {@link #startWork(java.util.Collection, LabWorkQueueParameters, org.broadinstitute.gpinformatics.mercury.entity.project.WorkflowDescription, org.broadinstitute.gpinformatics.mercury.entity.person.Person)} is called.
 */
public class FIFOLabWorkQueue<T extends LabWorkQueueParameters> extends FullAccessLabWorkQueue<T> {

    // order matters: fifo
    private List<WorkQueueEntry<T>> requestedWork = new ArrayList<WorkQueueEntry<T>>();

    private LabWorkQueueName name;
    
    private JiraService jiraService;
    
    public FIFOLabWorkQueue(LabWorkQueueName name,
                            JiraService jiraService) {
        if (name == null) {
             throw new NullPointerException("name cannot be null.");
        }
        this.name = name;
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
        JiraCommentUtil.postUpdate("Workflow update", user.getLogin() + " has created " + ticket.getTicketName() + " for the following samples:", vessels);
        JiraCommentUtil.postProjectOwnershipTableToTicket(vessels,ticket);

        return new JiraLabWorkQueueResponse("OK",ticket);
    }

    private CreateIssueResponse createJiraTicket(Collection<LabVessel> vessels,
                                                 WorkflowDescription workflowDescription,
                                                 T parameters,
                                                 JiraService jiraService) {
//        Collection<Project> allProjects = new HashSet<Project>();
        for (LabVessel vessel : vessels) {
            for (SampleInstance sampleInstance : vessel.getSampleInstances()) {
//                for (ProjectPlan projectPlan : sampleInstance.getAllProjectPlans()) {
//                    allProjects.add(projectPlan.getProject());
//                }
            }
        }

        String ticketTitle = null;
        StringBuilder ticketDetails = new StringBuilder();
//        if (allProjects.size() == 1) {
//            Project singleProject = allProjects.iterator().next();
//            ticketTitle = "Work for " + singleProject.getProjectName();
//            ticketDetails.append(singleProject.getProjectName());
//        }
//        else {
//            ticketTitle = "Work for " + allProjects.size() + " projects";
//            for (Project project : allProjects) {
//                ticketDetails.append(project.getProjectName()).append(" ");
//            }
//        }

        CreateIssueResponse jiraTicketCreationResponse =  null;

        try {

            /**
             * todo SGM.  Temporarily putting this in to support more generic approach to creating Jira Tickets.
             *
             */

            Map<String, CustomFieldDefinition> requiredFields=
                    jiraService.getRequiredFields(new CreateIssueRequest.Fields.Project(
                            CreateIssueRequest.Fields.ProjectType.LCSET_PROJECT_PREFIX.getKeyPrefix()),
                                              CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel);

            Collection<CustomField> customFieldList = new LinkedList<CustomField>();

            customFieldList.add(new CustomField(requiredFields.get("Protocol"),"test protocol"));
            customFieldList.add(new CustomField(requiredFields.get("Work Request ID(s)"),"WR 1 Billion!"));

            jiraTicketCreationResponse = jiraService.createIssue(workflowDescription.getJiraProjectPrefix(),
                    workflowDescription.getJiraIssueType(),
                    ticketTitle,
                    ticketDetails.toString(), customFieldList);
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
        // different {@link BasicProjectPlan}s, we play pin-the-tail-on-the-BasicProjectPlan
        // here, in FIFO order.
        boolean foundIt = false;
        for (WorkQueueEntry<T> queuedWork: requestedWork) {
            if (vessel.equals(queuedWork.getLabVessel())) {
                if (workflow.equals(queuedWork.getWorkflowDescription())) {
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
                break;
            }
        }
        if (!foundIt) {
            // todo log this somewhere
        }
    }
    
    private void startWorkflow(LabVessel vessel,/*BasicProjectPlan plan,*/T workflowParameters) {
        Collection<LabVessel> vessels = new ArrayList<LabVessel>(1);
        vessels.add(vessel);
    }


    @Override
    public LabWorkQueueResponse add(LabVessel vessel, 
                                    T workflowParameters, 
                                    WorkflowDescription workflowDescription/*,
                                    BasicProjectPlan projectPlanOverride*/) {
        if (vessel == null) {
             throw new NullPointerException("vessel cannot be null.");
        }
        if (workflowDescription == null) {
             throw new NullPointerException("workflowDescription cannot be null.");
        }
        WorkQueueEntry newWork = new WorkQueueEntry(this,vessel,workflowParameters,workflowDescription/*,projectPlanOverride*/);
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

    public void remove(WorkQueueEntry workQueueEntry) {
        if (!requestedWork.remove(workQueueEntry)) {
            throw new RuntimeException("WorkQueueEntry " + workQueueEntry + " was not part of the queued work for " + getQueueName());
        }
    }

    @Override
    public Collection<WorkQueueEntry<T>> getEntriesForWorkflow(WorkflowDescription workflow, LabVessel vessel) {
        final Collection<WorkQueueEntry<T>> workQueueEntries = new HashSet<WorkQueueEntry<T>>();
        for (WorkQueueEntry<T> workQueueEntry : requestedWork) {
            if (workQueueEntry.getWorkflowDescription().equals(workflow)) {
                if (workQueueEntry.getLabVessel().equals(vessel)) {
                    workQueueEntries.add(workQueueEntry);
                }
            }
        }
        return workQueueEntries;
    }
}
