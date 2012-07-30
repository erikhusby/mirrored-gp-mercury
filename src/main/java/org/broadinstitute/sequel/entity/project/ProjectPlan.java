package org.broadinstitute.sequel.entity.project;

import org.broadinstitute.sequel.entity.OrmUtil;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.workflow.LabBatch;
import org.broadinstitute.sequel.infrastructure.quote.Quote;
import org.broadinstitute.sequel.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.sequel.infrastructure.quote.QuoteServerException;
import org.broadinstitute.sequel.infrastructure.quote.QuoteService;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
public abstract class ProjectPlan {

    @Id
    @SequenceGenerator(name = "SEQ_PROJECT_PLAN", sequenceName = "SEQ_PROJECT_PLAN")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PROJECT_PLAN")
    private Long projectPlanId;

    // todo jmt fix this
    @Transient
    protected Collection<SequencingPlanDetail> planDetails = new HashSet<SequencingPlanDetail>();

    @ManyToMany(cascade = CascadeType.PERSIST)
    // have to specify name, generated name is too long for Oracle
    @JoinTable(name = "pp_map_start_sample_to_aliquot")
    // hbm2ddl always generates mapkey
    @MapKeyJoinColumn(name = "mapkey")
    private Map<StartingSample, LabVessel> mapStartingSampleToAliquot = new HashMap<StartingSample, LabVessel>();

    @ManyToMany(cascade = CascadeType.PERSIST)
    // have to specify name, generated name is too long for Oracle
    @JoinTable(name = "pp_map_start_vessel_to_aliquot")
    // hbm2ddl always generates mapkey
    @MapKeyJoinColumn(name = "mapkey")
    // For Oracle, this name must be <= 30 characters, including underscores from camel case conversion
    private Map<LabVessel, LabVessel> mapStartingVesselToAliquot = new HashMap<LabVessel, LabVessel>();

    @ManyToMany(cascade = CascadeType.PERSIST)
    private Set<StartingSample> startingSamples = new HashSet<StartingSample>();

    // have to specify name, generated name is too long for Oracle
    @JoinTable(name = "pp_starting_lab_vessels")
    @ManyToMany(cascade = CascadeType.PERSIST)
    private Set<LabVessel> startingLabVessels = new HashSet<LabVessel>();

    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private Project project;

    @ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private WorkflowDescription workflowDescription;

    protected ProjectPlan(Project project, WorkflowDescription workflowDescription) {
        if (project == null) {
            throw new NullPointerException("project cannot be null.");
        }
        if (workflowDescription == null) {
            throw new NullPointerException("workflowDescription cannot be null.");
        }
        this.project = project;
        this.workflowDescription = workflowDescription;
    }

    protected ProjectPlan() {
    }

    public abstract void addPlatingRequest(BSPPlatingRequest platingRequest);

    public abstract Collection<BSPPlatingRequest> getPendingPlatingRequests();

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public WorkflowDescription getWorkflowDescription() {
        return workflowDescription;
    }

    public void setWorkflowDescription(WorkflowDescription workflowDescription) {
        this.workflowDescription = workflowDescription;
    }

    public abstract Collection<ReagentDesign> getReagentDesigns();

    public abstract Collection<PoolGroup> getPoolGroups();

    public abstract Quote getQuoteDTO(QuoteService quoteService) throws QuoteServerException, QuoteNotFoundException;

    public Set<Starter> getStarters() {
        Set<Starter> starters = new HashSet<Starter>();
        starters.addAll(startingSamples);
        starters.addAll(startingLabVessels);
        return Collections.unmodifiableSet(starters);
    }

    public void addStarter(Starter starter) {
        if (starter == null) {
            throw new NullPointerException("vessel cannot be null.");
        }
        if(OrmUtil.proxySafeIsInstance(starter, StartingSample.class)) {
            startingSamples.add(OrmUtil.proxySafeCast(starter, StartingSample.class));
        } else if(OrmUtil.proxySafeIsInstance(starter, LabVessel.class)) {
            startingLabVessels.add(OrmUtil.proxySafeCast(starter, LabVessel.class));
        } else {
            throw new RuntimeException("Unexpected subclass " + starter.getClass());
        }
    }


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

    public LabVessel getAliquotForStarter(Starter starter) {
        if(OrmUtil.proxySafeIsInstance(starter, StartingSample.class)) {
            return mapStartingSampleToAliquot.get(OrmUtil.proxySafeCast(starter, StartingSample.class));
        } else if(OrmUtil.proxySafeIsInstance(starter, LabVessel.class)) {
            return mapStartingVesselToAliquot.get(OrmUtil.proxySafeCast(starter, LabVessel.class));
        } else {
            throw new RuntimeException("Unexpected subclass " + starter.getClass());
        }
    }

    public void addAliquotForStarter(Starter starter, LabVessel aliquot) {
        if (!getStarters().contains(starter)) {
            throw new RuntimeException(starter.getLabel() + " is not a starter for this project plan");
        }
        if(OrmUtil.proxySafeIsInstance(starter, StartingSample.class)) {
            mapStartingSampleToAliquot.put(OrmUtil.proxySafeCast(starter, StartingSample.class), aliquot);
        } else if(OrmUtil.proxySafeIsInstance(starter, LabVessel.class)) {
            mapStartingVesselToAliquot.put(OrmUtil.proxySafeCast(starter, LabVessel.class), aliquot);
        } else {
            throw new RuntimeException("Unexpected subclass " + starter.getClass());
        }
    }



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

