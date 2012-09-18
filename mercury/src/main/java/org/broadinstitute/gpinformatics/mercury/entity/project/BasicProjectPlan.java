package org.broadinstitute.gpinformatics.mercury.entity.project;

import org.broadinstitute.gpinformatics.mercury.entity.billing.Quote;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.mercury.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.mercury.infrastructure.quote.QuoteService;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
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
 * {@link org.broadinstitute.gpinformatics.mercury.entity.queue.LabWorkQueue}.  But
 * what, you ask, do we do to ensure that we track this "rework"
 * as a separate entity?  Answer: look at the {@link JiraTicket} that
 * was created when the lab work was started.
 *
 */
@Entity
@Audited
public class BasicProjectPlan extends ProjectPlan {

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

    @ManyToOne(fetch = FetchType.LAZY)
    private Quote quote;

    public BasicProjectPlan(Project project,
                            String name,
                            WorkflowDescription workflowDescription)  {
        super(project, workflowDescription);
        if (name == null) {
             throw new NullPointerException("name cannot be null."); 
        }
        this.planName = name;
        project.addProjectPlan(this);
    }

    protected BasicProjectPlan() {
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
    public org.broadinstitute.gpinformatics.mercury.infrastructure.quote.Quote getQuoteDTO(QuoteService quoteService) throws QuoteServerException, QuoteNotFoundException {
        org.broadinstitute.gpinformatics.mercury.infrastructure.quote.Quote quoteDTO = null;
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

    @Override
    public void doBilling(Starter starter, LabBatch labBatch,QuoteService quoteService) {
        throw new RuntimeException("I haven't been written yet.");
    }
}
