package org.broadinstitute.sequel.entity.project;

import org.broadinstitute.sequel.boundary.*;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.sequel.entity.bsp.BSPStartingSample;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.run.IlluminaSequencingTechnology;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.sequel.infrastructure.quote.*;

import javax.inject.Inject;
import java.math.BigInteger;
import java.util.*;

/**
 * A {@link ProjectPlan} that is backed by a
 * {@link AbstractPass}
 */
public class PassBackedProjectPlan implements ProjectPlan {

    private Set<BSPPlatingRequest> pendingPlatingRequests = new HashSet<BSPPlatingRequest>();

    private Set<JiraTicket> jiraTickets = new HashSet<JiraTicket>();

    private Map<Starter,LabVessel> aliquotForStarter = new HashMap<Starter, LabVessel>();
    private AbstractPass pass;

    private Project project;

    private Set<Starter> starters = new HashSet<Starter>();

    private Set<ReagentDesign> baits = new HashSet<ReagentDesign>();

    // todo arz pull out injected services and have constructors
    // take in pre-fetched DTOs (Quote, BSPStartingSample)

    @Inject
    private QuoteService quoteService;

    @Inject BSPSampleDataFetcher bspDataFetcher;

    private WorkflowDescription workflowDescription;

    private PercentXFoldCoverage percentXFoldCoverage;

    private Set<SequencingPlanDetail> sequencingPlans = new HashSet<SequencingPlanDetail>();

    public PassBackedProjectPlan() {}

    /**
     * Creates a new BasicProjectPlan from a pass.
     * @param pass
     * @param bspDataFetcher
     */
    public PassBackedProjectPlan(AbstractPass pass,
                                BSPSampleDataFetcher bspDataFetcher,
                                QuoteService quoteService,
                                BaitSetListResult baitsCache) {
        if (!(pass instanceof DirectedPass)) {
            throw new RuntimeException("SequeL can only deal with HS passes");
        }
        this.quoteService = quoteService;
        this.bspDataFetcher = bspDataFetcher;
        this.pass = pass;

        initSamples();
        initProject();
        initBaits((DirectedPass)pass,baitsCache);
        initWorkflow();
        initSequencePlanDetails();

        this.project.addProjectPlan(this);
    }

    private void initSequencePlanDetails() {
        CoverageAndAnalysisInformation coverageAndAnalysis = pass.getCoverageAndAnalysisInformation();
        TargetCoverageModel targetCoverage = coverageAndAnalysis.getTargetCoverageModel();
        if (targetCoverage == null) {
            throw new RuntimeException("Sorry, but SequeL only handles % @ xfold coverage.");
        }
        BigInteger percentCoverage = targetCoverage.getCoveragePercentage();
        BigInteger depth = targetCoverage.getDepth();
        percentXFoldCoverage = new PercentXFoldCoverage(percentCoverage.intValue(),depth.intValue());
        sequencingPlans.add(new SequencingPlanDetail(new IlluminaSequencingTechnology(),percentXFoldCoverage,this));

    }

    private void initWorkflow() {
        if (pass instanceof  DirectedPass) {
            DirectedPass hsPass = (DirectedPass)pass;
            workflowDescription =  new WorkflowDescription("Hybrid Selection",
                    new HashMap<LabEventName, org.broadinstitute.sequel.infrastructure.quote.PriceItem>(), null);
        }
    }

    private void initProject() {
        project = new BasicProject(pass.getResearchProject(),null);
    }

    private void initSamples() {
        final Set<String> bspSamples = new HashSet<String>();
        for (Sample passSample : pass.getSampleDetailsInformation().getSample()) {
            bspSamples.add(passSample.getBspSampleID());
        }
        final Map<String,BSPSampleDTO> sampleNameToSampleDTO = bspDataFetcher.fetchSamplesFromBSP(bspSamples);

        for (Sample passSample : pass.getSampleDetailsInformation().getSample()) {
            String bspSampleName = passSample.getBspSampleID();
            starters.add(new BSPStartingSample(bspSampleName, this, sampleNameToSampleDTO.get(bspSampleName)));
        }
    }

    private void initBaits(DirectedPass hsPass,
                               BaitSetListResult baitsCache) {
        Long baitSetId = hsPass.getBaitSetID();
        BaitSet bait = null;
        for (BaitSet baitSet : baitsCache.getBaitSetList()) {
            if (baitSetId.equals(baitSet.getId())) {
                bait = baitSet;
            }
        }
        if (bait == null) {
            throw new RuntimeException("Could not find bait with id " +baitSetId);
        }
        baits.add(new ReagentDesign(bait.getDesignName(), ReagentDesign.REAGENT_TYPE.BAIT));
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
        return baits;
    }

    @Override
    public Collection<PoolGroup> getPoolGroups() {
        return Collections.emptyList();
    }

    @Override
    public Quote getQuoteDTO() {
        Quote quote = null;
        try {
            quote = quoteService.getQuoteFromQuoteServer(pass.getFundingInformation().getSequencingQuoteID());
        }
        catch(Exception e) {
            throw new RuntimeException("Failed to fetch quote",e);
        }
        return quote;
    }

    @Override
    public Collection<Starter> getStarters() {
        return starters;
    }

    @Override
    public Collection<SequencingPlanDetail> getPlanDetails() {
        return sequencingPlans;
    }

    @Override
    public Collection<JiraTicket> getJiraTickets() {
        return jiraTickets;
    }

    @Override
    public String getName() {
        return pass.getProjectInformation().getPassNumber();
    }

    @Override
    public String getNotes() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public boolean isComplete(LabVessel startingVessel) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void setComplete(boolean isComplete) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void addSequencingDetail(SequencingPlanDetail sequencingDetail) {
        sequencingPlans.add(sequencingDetail);
    }

    @Override
    public void addJiraTicket(JiraTicket jiraTicket) {
        jiraTickets.add(jiraTicket);
    }

    @Override
    public void setAliquot(Starter starter, LabVessel aliquot) {
        if (!getStarters().contains(starter)) {
            throw new RuntimeException(starter.getLabel() + " is not a starter for this project plan");
        }
        aliquotForStarter.put(starter, aliquot);
    }

    @Override
    public LabVessel getAliquot(Starter starter) {
        return aliquotForStarter.get(starter);
    }

    public AbstractPass getPass() {
        return pass;
    }

    @Override
    public void addPlatingRequest(BSPPlatingRequest platingRequest) {
        pendingPlatingRequests.add(platingRequest);
    }

    @Override
    public Collection<BSPPlatingRequest> getPendingPlatingRequests() {
        return pendingPlatingRequests;
    }
}
