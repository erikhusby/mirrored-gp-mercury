package org.broadinstitute.sequel.entity.project;

import org.broadinstitute.sequel.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.infrastructure.quote.Quote;

import java.util.Collection;

public interface ProjectPlan {

    public void addPlatingRequest(BSPPlatingRequest platingRequest);

    public Collection<BSPPlatingRequest> getPendingPlatingRequests();

    public Project getProject();

    public WorkflowDescription getWorkflowDescription();

    public Collection<ReagentDesign> getReagentDesigns();

    public Collection<PoolGroup> getPoolGroups();

    public Quote getQuoteDTO();

    public Collection<Starter> getStarters();

    public Collection<SequencingPlanDetail> getPlanDetails();

    public Collection<JiraTicket> getJiraTickets();

    public String getName();

    public String getNotes();

    public boolean isComplete(LabVessel startingVessel);

    public void setComplete(boolean isComplete);

    public void addSequencingDetail(SequencingPlanDetail sequencingDetail);

    public void addJiraTicket(JiraTicket jiraTicket);

    public void setAliquot(Starter starter,LabVessel aliquot);

    public LabVessel getAliquot(Starter starter);
}

