package org.broadinstitute.sequel.entity.project;

import org.broadinstitute.sequel.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.infrastructure.quote.Quote;

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

    public abstract Quote getQuoteDTO();

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

