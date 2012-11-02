package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The batch of work, as tracked by a person
 * in the lab.  A batch is basically an lc set.
 */
@Entity
@Audited
@Table(schema = "mercury", uniqueConstraints = @UniqueConstraint(columnNames = {"batchName"}))
public class LabBatch {

    @Id
    @SequenceGenerator(name = "SEQ_LAB_BATCH", schema = "mercury", sequenceName = "SEQ_LAB_BATCH")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LAB_BATCH")
    private Long labBatchId;

    public static final String LCSET_PROJECT_PREFIX = "LCSET";

    @ManyToMany(cascade = CascadeType.PERSIST)
    // have to specify name, generated aud name is too long for Oracle
    @JoinTable(schema = "mercury", name = "lb_starting_lab_vessels")
    private Set<LabVessel> startingLabVessels = new HashSet<LabVessel>();

    private boolean isActive = true;

    private String batchName;

    @ManyToOne(fetch = FetchType.LAZY)
    private JiraTicket jiraTicket;

//    @ManyToOne(fetch = FetchType.LAZY)
//    private ProjectPlan projectPlan;

    // todo jmt get Hibernate to sort this
    @OneToMany(mappedBy = "labBatch")
    private Set<LabEvent> labEvents = new LinkedHashSet<LabEvent>();

    /**
     * Create a new batch with the given name
     * and set of @link Starter starting materials
     * @param batchName
     * @param starters
     */
//    public LabBatch(ProjectPlan projectPlan,
//                    String batchName,
//                    Set<Starter> starters) {
//        if (projectPlan == null) {
//            throw new NullPointerException("ProjectPlan cannot be null.");
//        }
//        if (batchName == null) {
//            throw new NullPointerException("BatchName cannot be null");
//        }
//        if (starters == null) {
//            throw new NullPointerException("starters cannot be null");
//        }
//        this.projectPlan = projectPlan;
//        this.batchName = batchName;
//        for (Starter starter : starters) {
//            addStarter(starter);
//        }
//    }

    public LabBatch(
            String batchName,
            Set<LabVessel> starters) {
        if (batchName == null) {
            throw new NullPointerException("BatchName cannot be null");
        }
        if (starters == null) {
            throw new NullPointerException("starters cannot be null");
        }
        this.batchName = batchName;
        for (LabVessel starter : starters) {
            addLabVessel(starter);
        }
    }


    protected LabBatch() {
    }

//    public ProjectPlan getProjectPlan() {
//        // todo could have different project plans per
//        // starter, make this a map accessible by Starter.
//        return projectPlan;
//    }

    public Set<LabVessel> getStartingLabVessels() {
        return startingLabVessels;
    }

    public void addLabVessel(LabVessel labVessel) {
        if (labVessel == null) {
            throw new NullPointerException("vessel cannot be null.");
        }
        startingLabVessels.add(labVessel);
        labVessel.addLabBatch(this);
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

//    public void setProjectPlanOverride(LabVessel vessel,ProjectPlan planOverride) {
//        throw new RuntimeException("I haven't been written yet.");
//    }

//    public ProjectPlan getProjectPlanOverride(LabVessel labVessel) {
//        throw new RuntimeException("I haven't been written yet.");
//    }

    public Set<LabEvent> getLabEvents() {
        return labEvents;
    }

    public void setLabEvents(Set<LabEvent> labEvents) {
        this.labEvents = labEvents;
    }
}
