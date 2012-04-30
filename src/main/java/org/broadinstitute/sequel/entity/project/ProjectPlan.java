package org.broadinstitute.sequel.entity.project;

import org.broadinstitute.sequel.entity.billing.Quote;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.sequel.entity.vessel.LabVessel;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Collection;
import java.util.HashSet;


/**
 * A {@link ProjectPlan} defines what a customer would
 * like us to do.  <b>It doesn't change unless the lab customer's
 * requirements change.</b>  This means that if the customer says
 * the want 20x sequencing and we flub it and only deliver 5x on the
 * first try, <b>we <i>don't</i> make another {@link ProjectPlan} for 15x.</b>
 * What we do instead is just ask the lab to do more work, but still
 * on behalf of the same {@link ProjectPlan}.  How do we do this?
 * 
 * Add the appropriate {@link LabVessel}s into the appropriate
 * {@link org.broadinstitute.sequel.entity.queue.LabWorkQueue}.  But
 * what, you ask, do we do to ensure that we track this "rework"
 * as a separate entity?  Answer: look at the {@link JiraTicket} that
 * was created when the lab work was started.
 *
 */
@Entity
public class ProjectPlan {
    @Id
    private Long projectPlanId;

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
    
    public org.broadinstitute.sequel.infrastructure.quote.Quote getQuoteDTO() {
        org.broadinstitute.sequel.infrastructure.quote.Quote quoteDTO = null;
        if (quote != null) {
            quoteDTO = quote.getQuote();
        }
        return quoteDTO;
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
        // should this also link this ticket to
        // the project's ticket?
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

    public boolean isComplete(LabVessel startingVessel) {
        throw new RuntimeException("I haven't been written yet.");
    }

    public void setComplete(boolean isComplete) {
        throw new RuntimeException("I haven't been written yet.");
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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProjectPlan that = (ProjectPlan) o;

        if (planName != null ? !planName.equals(that.planName) : that.planName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return planName != null ? planName.hashCode() : 0;
    }

    public String toString() {
        return planName;
    }

}
