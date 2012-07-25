package org.broadinstitute.sequel.entity.workflow;

import org.broadinstitute.sequel.entity.OrmUtil;
import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.project.Starter;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.vessel.LabVessel;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The batch of work, as tracked by a person
 * in the lab.  A batch is basically an lc set.
 */
@Entity
public class LabBatch {

    @Id
    @SequenceGenerator(name = "SEQ_LAB_BATCH", sequenceName = "SEQ_LAB_BATCH")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LAB_BATCH")
    private Long labBatchId;

    public static final String LCSET_PROJECT_PREFIX = "LCSET";

    @ManyToMany(cascade = CascadeType.PERSIST)
    private Set<StartingSample> startingSamples = new HashSet<StartingSample>();

    @ManyToMany(cascade = CascadeType.PERSIST)
    private Set<LabVessel> startingLabVessels = new HashSet<LabVessel>();

    private boolean isActive = true;

    private String batchName;

    @ManyToOne(fetch = FetchType.LAZY)
    private JiraTicket jiraTicket;

    @ManyToOne(fetch = FetchType.LAZY)
    private ProjectPlan projectPlan;

    /**
     * Create a new batch with the given name
     * and set of {@link Starter starting materials}
     * @param batchName
     * @param starters
     */
    public LabBatch(ProjectPlan projectPlan,
                    String batchName,
                    Set<Starter> starters) {
        if (projectPlan == null) {
            throw new NullPointerException("ProjectPlan cannot be null.");
        }
        if (batchName == null) {
            throw new NullPointerException("BatchName cannot be null");
        }
        if (starters == null) {
            throw new NullPointerException("starters cannot be null");
        }
        this.projectPlan = projectPlan;
        this.batchName = batchName;
        for (Starter starter : starters) {
            addStarter(starter);
        }
    }

    protected LabBatch() {
    }

    public ProjectPlan getProjectPlan() {
        // todo could have different project plans per
        // starter, make this a map accessible by Starter.
        return projectPlan;
    }

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

    public boolean getActive() {
        return isActive;
    }
    
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
    
    public String getBatchName() {
        return batchName;
    }

    public void setJiraTicket(JiraTicket jiraTicket) {
        this.jiraTicket = jiraTicket;
    }

    public JiraTicket getJiraTicket() {
        return jiraTicket;
    }

    public void setProjectPlanOverride(LabVessel vessel,ProjectPlan planOverride) {
        throw new RuntimeException("I haven't been written yet.");
    }

    public ProjectPlan getProjectPlanOverride(LabVessel labVessel) {
        throw new RuntimeException("I haven't been written yet.");
    }

}
