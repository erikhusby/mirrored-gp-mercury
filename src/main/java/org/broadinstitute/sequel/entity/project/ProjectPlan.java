package org.broadinstitute.sequel.entity.project;

import org.broadinstitute.sequel.control.quote.Quote;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.sequel.entity.labevent.LabEventType;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.hibernate.loader.collection.CollectionInitializer;

import java.util.Collection;
import java.util.HashSet;


public class ProjectPlan {

    private Collection<LabVessel> starters = new HashSet<LabVessel>();
    
    private Collection<SequencingPlanDetail> planDetails = new HashSet<SequencingPlanDetail>();

    private WorkflowDescription workflowDescription;

    private Project project;
    
    private String planName;
    
    private String notes;

    // todo where does analysis type go here?

    private Collection<PoolGroup> poolGroups = new HashSet<PoolGroup>();

    private Collection<ReagentDesign> reagentDesigns = new HashSet<ReagentDesign>();

    private final Collection<BSPPlatingRequest> platingRequests = new HashSet<BSPPlatingRequest>();
    
    private final Collection<JiraTicket> jiraTickets = new HashSet<JiraTicket>();

    private Quote quote;
    
    public ProjectPlan(Project project,
                       String name,
                       WorkflowDescription workflowDescription)  {
        if (project == null) {
             throw new NullPointerException("project cannot be null."); 
        }
        if (name == null) {
             throw new NullPointerException("name cannot be null."); 
        }
        if (workflowDescription == null) {
             throw new NullPointerException("workflowDescription cannot be null.");
        }
        this.project = project;
        this.planName = name;
        this.workflowDescription = workflowDescription;
        project.addProjectPlan(this);
    }

    public Project getProject() {
        return project;
    }

    public WorkflowDescription getWorkflowDescription() {
        return workflowDescription;
    }
    
    public Collection<ReagentDesign> getReagentDesigns() {
        return reagentDesigns;
    }
    
    public void addReagentDesign(ReagentDesign design) {
        reagentDesigns.add(design);
    }
    
    public void addPoolGroup(PoolGroup poolGroup) {
        if (poolGroup == null) {
             throw new NullPointerException("poolGroup cannot be null."); 
        }
        poolGroups.add(poolGroup);
    }

    public Collection<PoolGroup> getPoolGroups() {
        return poolGroups;
    }
    
    public Quote getQuote() {
        return quote;
    }
    
    public void setQuote(Quote quote) {
        this.quote = quote;
    }
    
    public void addStarter(LabVessel vessel) {
        if (vessel == null) {
            throw new NullPointerException("vessel cannot be null.");
        }
        project.addStarter(vessel);
        starters.add(vessel);
    }
    
    public Collection<LabVessel> getStarters() {
        return starters;
    }

    public void addSequencingDetail(SequencingPlanDetail detail) {
        if (detail == null) {
             throw new NullPointerException("detail cannot be null.");
        }
        planDetails.add(detail);
    }

    public Collection<SequencingPlanDetail> getPlanDetails() {
        return planDetails;
    }

    public void addPlatingRequest(BSPPlatingRequest platingRequest) {
        if (platingRequest == null) {
            throw new IllegalArgumentException("platingRequest must be non-null in AbstractProject.addPlatingRequest");
        }
        platingRequests.add(platingRequest);
    }

    public Collection<BSPPlatingRequest> getPendingPlatingRequests() {
        final Collection<BSPPlatingRequest> pendingRequests = new HashSet<BSPPlatingRequest>();
        for (BSPPlatingRequest platingRequest : platingRequests) {
            if (!platingRequest.isFulfilled()) {
                pendingRequests.add(platingRequest);
            }
        }
        return pendingRequests;
    }
    
    public void addJiraTicket(JiraTicket jiraTicket) {
        jiraTickets.add(jiraTicket);
    }

    /**
     * What are all the jira tickets that were used
     * for this plan?
     * @return
     */
    public Collection<JiraTicket> getJiraTickets() {
        return jiraTickets;
    }

    /**
     * What's the name of this plan?
     * @return
     */
    public String getName() {
        return planName;
    }
    
    public String getNotes() {
        return notes;
    }

    /**
     * Basically how much sequencing are we going
     * to do for this project?  We assume that the
     * sequencing goal is uniform for every sample
     * in the project.
     * @return
     */

    // todo way to abstract categories for weekly prioritization/capacity
    // planning from critter.

    // organism, collaborator, initiative/funding source/quote, prep
    // type, sequencing type, outbreak,

}
