package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.io.IOException;
import java.util.*;

/**
 * The batch of work, as tracked by a person
 * in the lab.  A batch is basically an lc set.
 */
@Entity
@Audited
@Table(schema = "mercury", uniqueConstraints = @UniqueConstraint(columnNames = {"batchName"}))
public class LabBatch {

    public static final Comparator<LabBatch> byDate = new Comparator<LabBatch>() {
        @Override
        public int compare(LabBatch bucketEntryPrime, LabBatch bucketEntrySecond) {
            return bucketEntryPrime.getCreatedOn().compareTo(bucketEntrySecond.getCreatedOn());
        }
    };

    @Id
    @SequenceGenerator(name = "SEQ_LAB_BATCH", schema = "mercury", sequenceName = "SEQ_LAB_BATCH")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LAB_BATCH")
    private Long labBatchId;

    @ManyToMany(cascade = CascadeType.PERSIST)
    // have to specify name, generated aud name is too long for Oracle
    @JoinTable(schema = "mercury", name = "lb_starting_lab_vessels")
    private Set<LabVessel> startingLabVessels = new HashSet<LabVessel>();

    private boolean isActive = true;

    private String batchName;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private JiraTicket jiraTicket;

    //    @ManyToOne(fetch = FetchType.LAZY)
    //    private ProjectPlan projectPlan;

    // todo jmt get Hibernate to sort this
    @OneToMany(mappedBy = "labBatch")
    private Set<LabEvent> labEvents = new LinkedHashSet<LabEvent>();

    private Date createdOn;

    //TODO SGM Add field for Description for Jira Creation Override

    /**
     * Create a new batch with the given name
     * and set of @link Starter starting materials
     *
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
    public LabBatch(String batchName, Set<LabVessel> starters) {
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
        createdOn = new Date();
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
        jiraTicket.setLabBatch(this);
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

    public void addLabEvent(LabEvent labEvent) {
        this.labEvents.add(labEvent);
    }

    public void addLabEvents(Set<LabEvent> labEvents) {
        this.labEvents.addAll(labEvents);
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    /**
     * Helper nethod to dynamically create batch names based on Input from PDM's.  The format for the Names of the
     * batches, when not manually defined, will be:
     *
     * [Product name] [Product workflow Version]: [comma separated list of PDO names]
     *
     * @param workflowName
     * @param pdoNames
     * @return
     */
    public static String generateBatchName(@Nonnull String workflowName, @Nonnull Collection<String> pdoNames) {

        StringBuilder batchName = new StringBuilder();

        batchName.append(workflowName).append(": ");
        boolean first = true;

        for (String currentPdo : pdoNames) {
            if (!first) {
                batchName.append(", ");
            }

            batchName.append(currentPdo);
            first = false;
        }

        return batchName.toString();
    }

    /**
     * RequiredSubmissionFields is an enum intended to assist in the creation of a Jira ticket
     * for Product orders
     */
    public enum RequiredSubmissionFields implements CustomField.SubmissionField {
        PROTOCOL("Protocol", true),

        //Will not have WR ID info in Mercury.  Set to a Blank string
        WORK_REQUEST_IDS("Work Request ID(s)", true),
        POOLING_STATUS("Pooling Status", true),
        PRIORITY("priority", false),
        DUE_DATE("duedate", false),

        //User comments at batch creation (Post Dec 1 addition)
        IMPORTANT("Important", true),

        // ??
        NUMBER_OF_CONTROLS("Number of Controls", true),
        NUMBER_OF_SAMPLES("Number of Samples", true),

        //        DO not set this value.  Leave at it's default (for now)
        LIBRARY_QC_SEQUENCING_REQUIRED("Library QC Sequencing Required?", true),

        //Radio Button custom field
        PROGRESS_STATUS("Progress Status", true),

        //List of Sample names
        GSSR_IDS("GSSR ID(s)", true),;

        private final String  fieldName;
        private final boolean customField;

        private RequiredSubmissionFields(String fieldNameIn, boolean customFieldInd) {
            fieldName = fieldNameIn;
            customField = customFieldInd;
        }

        @Nonnull
        @Override
        public String getFieldName() {
            return fieldName;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof LabBatch))
            return false;

        LabBatch labBatch = (LabBatch) o;

        if (isActive != labBatch.isActive)
            return false;
        if (batchName != null ? !batchName.equals(labBatch.batchName) : labBatch.batchName != null)
            return false;
        if (jiraTicket != null ? !jiraTicket.equals(labBatch.jiraTicket) : labBatch.jiraTicket != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (isActive ? 1 : 0);
        result = 31 * result + (batchName != null ? batchName.hashCode() : 0);
        result = 31 * result + (jiraTicket != null ? jiraTicket.hashCode() : 0);
        return result;
    }
}
