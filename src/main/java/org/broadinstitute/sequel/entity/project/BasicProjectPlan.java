package org.broadinstitute.sequel.entity.project;

import org.broadinstitute.sequel.entity.billing.Quote;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.workflow.LabBatch;
import org.broadinstitute.sequel.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.sequel.infrastructure.quote.QuoteServerException;
import org.broadinstitute.sequel.infrastructure.quote.QuoteService;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import java.util.*;


/**
 * A {@link BasicProjectPlan} defines what a customer would
 * like us to do.  <b>It doesn't change unless the lab customer's
 * requirements change.</b>  This means that if the customer says
 * the want 20x sequencing and we flub it and only deliver 5x on the
 * first try, <b>we <i>don't</i> make another {@link BasicProjectPlan} for 15x.</b>
 * What we do instead is just ask the lab to do more work, but still
 * on behalf of the same {@link BasicProjectPlan}.  How do we do this?
 * 
 * Add the appropriate {@link LabVessel}s into the appropriate
 * {@link org.broadinstitute.sequel.entity.queue.LabWorkQueue}.  But
 * what, you ask, do we do to ensure that we track this "rework"
 * as a separate entity?  Answer: look at the {@link JiraTicket} that
 * was created when the lab work was started.
 *
 */
@Entity
public class BasicProjectPlan extends ProjectPlan {

    @Transient // todo arz fix me
    private Map<Starter,LabVessel> aliquotForStarter = new HashMap<Starter, LabVessel>();

    @Transient
    private Collection<Starter> starters = new HashSet<Starter>();

    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    protected WorkflowDescription workflowDescription;

    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    protected Project project;
    
    protected String planName;
    
    private String notes;

    // todo where does analysis type go here?

    // todo jmt fix this
    @Transient
    private Collection<PoolGroup> poolGroups = new HashSet<PoolGroup>();

    // todo jmt fix this
    @Transient
    private Collection<ReagentDesign> reagentDesigns = new HashSet<ReagentDesign>();

    // todo jmt fix this
    @Transient
    private final Collection<BSPPlatingRequest> platingRequests = new HashSet<BSPPlatingRequest>();

    @OneToMany
    private final Collection<JiraTicket> jiraTickets = new HashSet<JiraTicket>();

    @ManyToOne(fetch = FetchType.LAZY)
    private Quote quote;

    //@OneToMany(mappedBy = "projectPlan")
    @Transient
    private Set<StartingSample> startingSamples = new HashSet<StartingSample>();


    public BasicProjectPlan(Project project,
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

    protected BasicProjectPlan() {
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public WorkflowDescription getWorkflowDescription() {
        return workflowDescription;
    }
    
    @Override
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

    @Override
    public Collection<PoolGroup> getPoolGroups() {
        return poolGroups;
    }
    
    @Override
    public org.broadinstitute.sequel.infrastructure.quote.Quote getQuoteDTO(QuoteService quoteService) throws QuoteServerException, QuoteNotFoundException {
        org.broadinstitute.sequel.infrastructure.quote.Quote quoteDTO = null;
        if (quote != null) {
            quoteDTO = quote.getQuote();
        }
        else {
            if (quoteService != null) {
                quoteDTO = quoteService.getQuoteFromQuoteServer(quote.getAlphanumericId());
            }
        }
        return quoteDTO;
    }
    
    public void setQuote(Quote quote) {
        this.quote = quote;
    }
    
    public void addStarter(Starter starter) {
        if (starter == null) {
            throw new NullPointerException("vessel cannot be null.");
        }
        project.addStarter(starter);
        starters.add(starter);
    }
    
    @Override
    public Collection<Starter> getStarters() {
        return starters;
    }

    public void addSequencingDetail(SequencingPlanDetail detail) {
        if (detail == null) {
             throw new NullPointerException("detail cannot be null.");
        }
        planDetails.add(detail);
    }

    public void addPlatingRequest(BSPPlatingRequest platingRequest) {
        if (platingRequest == null) {
            throw new IllegalArgumentException("platingRequest must be non-null in BasicProjectPlan.addPlatingRequest");
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
    @Override
    public Collection<JiraTicket> getJiraTickets() {
        return jiraTickets;
    }

    /**
     * What's the name of this plan?
     * @return
     */
    @Override
    public String getName() {
        return planName;
    }
    
    @Override
    public String getNotes() {
        return notes;
    }

    @Override
    public boolean isComplete(LabVessel startingVessel) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
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

        BasicProjectPlan that = (BasicProjectPlan) o;

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

    public Set<StartingSample> getStartingSamples() {
        return startingSamples;
    }

    @Override
    public void setAliquot(Starter starter, LabVessel aliquot) {
        if (!getStarters().contains(starter)) {
            throw new RuntimeException(starter.getLabel() + " is not a starter for this project plan");
        }
        aliquotForStarter.put(starter,aliquot);
    }

    @Override
    public LabVessel getAliquot(Starter starter) {
        return aliquotForStarter.get(starter);
    }

    @Override
    public void doBilling(Starter starter, LabBatch labBatch,QuoteService quoteService) {
        throw new RuntimeException("I haven't been written yet.");
    }
}
