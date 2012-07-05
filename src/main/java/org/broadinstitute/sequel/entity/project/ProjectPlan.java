package org.broadinstitute.sequel.entity.project;

import org.broadinstitute.sequel.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.workflow.LabBatch;
import org.broadinstitute.sequel.infrastructure.quote.Quote;
import org.broadinstitute.sequel.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.sequel.infrastructure.quote.QuoteServerException;
import org.broadinstitute.sequel.infrastructure.quote.QuoteService;

import javax.persistence.Transient;
import java.util.Collection;
import java.util.HashSet;

public abstract class ProjectPlan {

    // todo jmt fix this
    @Transient
    protected Collection<SequencingPlanDetail> planDetails = new HashSet<SequencingPlanDetail>();


    public abstract void addPlatingRequest(BSPPlatingRequest platingRequest);

    public abstract Collection<BSPPlatingRequest> getPendingPlatingRequests();

    public abstract Project getProject();

    public abstract WorkflowDescription getWorkflowDescription();

    public abstract Collection<ReagentDesign> getReagentDesigns();

    public abstract Collection<PoolGroup> getPoolGroups();

    public abstract Quote getQuoteDTO(QuoteService quoteService) throws QuoteServerException, QuoteNotFoundException;

    public abstract Collection<Starter> getStarters();

    public Collection<SequencingPlanDetail> getPlanDetails() {
        return planDetails;
    }

    public abstract Collection<JiraTicket> getJiraTickets();

    public abstract String getName();

    public abstract String getNotes();

    public abstract boolean isComplete(LabVessel startingVessel);

    public abstract void setComplete(boolean isComplete);

    public abstract void addSequencingDetail(SequencingPlanDetail sequencingDetail);

    public abstract void addJiraTicket(JiraTicket jiraTicket);

    public abstract void setAliquot(Starter starter,LabVessel aliquot);

    public abstract LabVessel getAliquot(Starter starter);

    /**
     * Do the billing for the given {@link Starter}.  {@link LabBatch} is a temporary
     * stub so that we can have something to link back to from the quote server.
     * See {@link org.broadinstitute.sequel.infrastructure.quote.QuoteService#registerNewWork(org.broadinstitute.sequel.infrastructure.quote.Quote, org.broadinstitute.sequel.infrastructure.quote.PriceItem, double, String, String, String)}, especially the callback
     * url.  Eventually the callback will be the SequeL UI, but for the moment,
     * we'll just redirect to jira so we can demo this to sheila.
     * @param starter
     * @param labBatch
     */
    public abstract void doBilling(Starter starter,LabBatch labBatch,QuoteService quoteService);

    /**
     * getLaneCoverage is a helper method which allows a user to find the specified lane coverage (If any) in the
     * coverage goals defined in the collection of Sequencing Plans.
     * @return an integer that represents the number of lanes specified for sequencing material associated with a
     * specific project plan
     */
    @Transient
    public int getLaneCoverage() {

        int numberOfLanes = 0;
        for(SequencingPlanDetail projPlanDetail:this.getPlanDetails()) {
            if(projPlanDetail.getCoverageGoal() instanceof NumberOfLanesCoverage) {
                numberOfLanes = Integer.parseInt(projPlanDetail.getCoverageGoal().coverageGoalToParsableText());
                break;
            }
        }
        return numberOfLanes;
    }

    /**
     * getReadLength is a helper method which allows a user to find the specified Read Length (If any) in the
     * coverage goals defined in the collection of Sequencing Plans.
     * @return an integer that represents the Read Length specified for sequencing material associated with a
     * specific project plan
     */
    @Transient
    public int getReadLength() {

        int readLength = 0;
        for(SequencingPlanDetail projPlanDetail:this.getPlanDetails()) {
            if(projPlanDetail.getCoverageGoal() instanceof PairedReadCoverage) {
                readLength = Integer.parseInt(projPlanDetail.getCoverageGoal().coverageGoalToParsableText());
                break;
            }
        }
        return readLength;
    }
}

