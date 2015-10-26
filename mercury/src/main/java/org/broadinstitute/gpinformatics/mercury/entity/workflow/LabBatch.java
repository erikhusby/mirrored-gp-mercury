package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "labBatch")
    private Set<LabBatchStartingVessel> startingBatchLabVessels = new HashSet<>();

    @Deprecated
    @ManyToMany(cascade = CascadeType.PERSIST)
    // have to specify name, generated aud name is too long for Oracle
    @JoinTable(schema = "mercury", name = "lb_starting_lab_vessels")
    private Set<LabVessel> startingLabVessels = new HashSet<>();

    private boolean isActive = true;

    private String batchName;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private JiraTicket jiraTicket;

    // todo jmt get Hibernate to sort this
    @OneToMany(mappedBy = "labBatch", cascade = CascadeType.PERSIST)
    private Set<LabEvent> labEvents = new LinkedHashSet<>();

    /**
     * Vessels in the batch that were added as rework from a previous batch.
     */
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "lab_batch_reworks", joinColumns = @JoinColumn(name = "lab_batch"),
            inverseJoinColumns = @JoinColumn(name = "reworks"))
    private Collection<LabVessel> reworks = new HashSet<>();

    private Date createdOn;

    private Boolean isValidationBatch;

    // If we store this as Workflow in the database, we need to determine the best way to store 'no workflow'.
    private String workflowName;

    /**
     * needed for fix-up test
     */
    protected void setLabBatchType(LabBatchType labBatchType) {
        this.labBatchType = labBatchType;
    }

    public enum LabBatchType {
        /**
         * A batch created as part of workflow, e.g. an LCSET
         */
        WORKFLOW,
        /**
         * A batch created in BSP, typically named BP-1234
         */
        BSP,
        /**
         * Receipt of samples into BSP, from external collaborators, typically named SK-1234 for sample kit
         */
        SAMPLES_RECEIPT,
        /**
         * Import of BSP samples into Mercury
         */
        SAMPLES_IMPORT,
        /**
         * Flowcell Tracking batch (FCT)
         */
        FCT,
        /**
         * MISEQ FCT Batch
         */
        MISEQ
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LabBatchType labBatchType;

    @Enumerated(EnumType.STRING)
    private IlluminaFlowcell.FlowcellType flowcellType;

    @Transient
    private String batchDescription;

    @Transient
    private Date dueDate;

    @Transient
    private String important;

    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "labBatch")
    private Set<BucketEntry> bucketEntries = new HashSet<>();

    protected LabBatch() {
    }

    public LabBatch(@Nonnull String batchName, @Nonnull Set<LabVessel> starterVessels,
                    @Nonnull LabBatchType labBatchType) {
        this(batchName, starterVessels, labBatchType, null);
    }

    public LabBatch(@Nonnull String batchName, @Nonnull Set<LabVessel> starterVessels,
                    @Nonnull LabBatchType labBatchType, @Nullable BigDecimal concentration) {
        this.batchName = batchName;
        this.labBatchType = labBatchType;
        for (LabVessel starter : starterVessels) {
            addLabVessel(starter, concentration);
        }
        createdOn = new Date();
    }

    public LabBatch(@Nonnull String batchName, @Nonnull Set<LabVessel> starterVessels,
                    @Nonnull LabBatchType labBatchType, @Nullable BigDecimal concentration,
                    @Nullable IlluminaFlowcell.FlowcellType flowcellType) {
        this.batchName = batchName;
        this.labBatchType = labBatchType;
        this.flowcellType = flowcellType;
        for (LabVessel starter : starterVessels) {
            addLabVessel(starter, concentration);
        }
        createdOn = new Date();
    }

    public LabBatch(@Nonnull String batchName, @Nonnull Set<LabVessel> startingBatchLabVessels,
                    @Nonnull LabBatchType labBatchType,
                    String batchDescription, Date dueDate, String important) {

        this(batchName, startingBatchLabVessels, labBatchType);
        this.batchDescription = batchDescription;
        this.dueDate = dueDate;
        this.important = important;
    }

    public LabBatch(@Nonnull String batchName, @Nonnull Set<LabVessel> startingLabVessels,
                    Set<LabVessel> reworkLabVessels,
                    @Nonnull LabBatchType labBatchType, String workflowName, String batchDescription, Date dueDate,
                    String important) {
        this(batchName, startingLabVessels, labBatchType, batchDescription, dueDate, important);
        this.workflowName = workflowName;
        addReworks(reworkLabVessels);
    }

    /**
     * Adds the given rework vessels to the list of reworks for the batch.
     *
     * @param newRework
     */
    public void addReworks(Collection<LabVessel> newRework) {
        reworks.addAll(newRework);
        for (LabVessel rework : newRework) {
            rework.addReworkLabBatch(this);
        }
    }

    public Collection<LabVessel> getReworks() {
        return reworks;
    }

    /**
     * Returns all starting vessels in the batch, including reworks.
     *
     * @return the vessels in the batch
     */
    public Set<LabVessel> getStartingBatchLabVessels() {
        Set<LabVessel> allStartingLabVessels = new HashSet<>();
        allStartingLabVessels.addAll(getNonReworkStartingLabVessels());
        allStartingLabVessels.addAll(reworks);
        return allStartingLabVessels;
    }

    public Set<LabVessel> getNonReworkStartingLabVessels() {
        Set<LabVessel> staringBatchVessels = new HashSet<>();
        for (LabBatchStartingVessel batchStartingVessel : startingBatchLabVessels) {
            staringBatchVessels.add(batchStartingVessel.getLabVessel());
        }
        return staringBatchVessels;
    }

    public Set<LabBatchStartingVessel> getLabBatchStartingVessels() {
        return startingBatchLabVessels;
    }

    public void addLabVessel(@Nonnull LabVessel labVessel) {
        addLabVessel(labVessel, null);
    }

    public void addLabVessel(@Nonnull LabVessel labVessel, @Nullable BigDecimal concentration) {
        LabBatchStartingVessel labBatchStartingVessel = new LabBatchStartingVessel(labVessel, this, concentration);
        startingBatchLabVessels.add(labBatchStartingVessel);
        labVessel.addNonReworkLabBatchStartingVessel(labBatchStartingVessel);
    }

    public void addLabVessels(@Nonnull Collection<LabVessel> vessels) {
        for (LabVessel currVessel : vessels) {
            addLabVessel(currVessel);
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

    /**
     * For fixups only.
     */
    void setBatchName(String batchName) {
        this.batchName = batchName;
    }

    public void setJiraTicket(JiraTicket jiraTicket) {
        this.jiraTicket = jiraTicket;
        jiraTicket.setLabBatch(this);

        batchName = this.jiraTicket.getTicketName();
    }

    public Set<LabVessel> getStartingLabVessels() {
        return startingLabVessels;
    }

    public void setStartingLabVessels(Set<LabVessel> oldStartingLabVessels) {
        this.startingLabVessels = oldStartingLabVessels;
    }

    public JiraTicket getJiraTicket() {
        return jiraTicket;
    }

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

    private List<LabEvent> getAllEventsSortedByDate() {
        Map<Date, LabEvent> sortedTreeMap = new TreeMap<>();
        for (LabEvent event : getLabEvents()) {
            sortedTreeMap.put(event.getEventDate(), event);
        }
        return new ArrayList<>(sortedTreeMap.values());
    }

    public LabEvent getLatestEvent() {
        LabEvent event = null;
        List<LabEvent> eventsList = getAllEventsSortedByDate();

        int size = eventsList.size();
        if (size > 0) {
            event = eventsList.get(size - 1);
        }
        return event;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public boolean isValidationBatch() {
        return isValidationBatch == null ? false : isValidationBatch;
    }

    public void setValidationBatch(Boolean validationBatch) {
        isValidationBatch = validationBatch;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public void setWorkflow(@Nonnull Workflow workflow) {
        workflowName = workflow.getWorkflowName();
    }

    public String getBatchDescription() {
        return batchDescription;
    }

    public void setBatchDescription(String batchDescription) {
        this.batchDescription = batchDescription;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public String getImportant() {
        return important;
    }

    public void setImportant(String important) {
        this.important = important;
    }

    /**
     * Helper method to dynamically create batch names based on Input from PDMs.  The format for the Names of the
     * batches, when not manually defined, will be:
     * <p/>
     * {@code [Product name] [Product workflow Version]: [comma separated list of PDO names]}
     */
    public static String generateBatchName(@Nonnull Workflow workflow, @Nonnull Collection<String> pdoNames) {

        StringBuilder batchName = new StringBuilder();

        batchName.append(workflow.getWorkflowName()).append(": ");
        boolean first = true;

        for (String currentPdo : pdoNames) {
            if (!first) {
                batchName.append(", ");
            }

            batchName.append(currentPdo);
            first = false;
        }

        return batchName.toString().trim();
    }

    public String getBusinessKey() {
        return batchName;
    }

    public Long getLabBatchId() {
        return labBatchId;
    }

    public LabBatchType getLabBatchType() {
        return labBatchType;
    }

    public IlluminaFlowcell.FlowcellType getFlowcellType() {
        return flowcellType;
    }

    /**
     * TicketFields is an enum intended to assist in the creation of a Jira ticket
     * for Product orders
     */
    public enum TicketFields implements CustomField.SubmissionField {
        PROTOCOL("Protocol", true),

        // Will not have WR ID info in Mercury.  Set to a Blank string
        WORK_REQUEST_IDS("Work Request ID(s)", true),
        POOLING_STATUS("Pooling Status", true),
        PRIORITY("Priority", false),
        DUE_DATE("Due Date", false),

        // User comments at batch creation (Post Dec 1 addition)
        IMPORTANT("Important", true),
        DESCRIPTION("Description", true),

        NUMBER_OF_CONTROLS("Number of Controls", true),
        NUMBER_OF_SAMPLES("Number of Samples", true),
        SAMPLE_IDS("Sample IDs", true),
        BATCH_TYPE("Batch Type", true),

        // DO not set this value.  Leave at its default (for now).
        LIBRARY_QC_SEQUENCING_REQUIRED("Library QC Sequencing Required?", true),

        // Radio Button custom field
        PROGRESS_STATUS("Progress Status", true),

        // List of Sample names
        GSSR_IDS("GSSR ID(s)", true),

        LIMS_ACTIVITY_STREAM("LIMS Activity Stream", true),
        SUMMARY("Summary", false),
        SEQUENCING_STATION("Sequencing Station", true),
        MATERIAL_TYPE("BATCH_TYPE", true);


        private final String fieldName;
        private final boolean customField;
        private final boolean nullable;

        private TicketFields(String fieldNameIn, boolean customFieldInd) {
            this(fieldNameIn, customFieldInd, false);
        }
        private TicketFields(String fieldNameIn, boolean customFieldInd, boolean nullable) {
            fieldName = fieldNameIn;
            customField = customFieldInd;
            this.nullable = nullable;
        }

        @Override
        public boolean isNullable() {
            return nullable;
        }


        @Nonnull
        @Override
        public String getName() {
            return fieldName;
        }
    }

    public LabVessel[] getStartingVesselsArray() {
        return startingBatchLabVessels.toArray(new LabVessel[startingBatchLabVessels.size()]);
    }

    public Set<BucketEntry> getBucketEntries() {
        return bucketEntries;
    }

    public void addBucketEntry(BucketEntry bucketEntry) {
        bucketEntries.add(bucketEntry);
        bucketEntry.setLabBatch(this);
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LabBatch)) {
            return false;
        }

        LabBatch labBatch = (LabBatch) o;

        EqualsBuilder equalsBuilder = new EqualsBuilder();

        equalsBuilder.append(isActive, labBatch.getActive());
        equalsBuilder.append(batchName, labBatch.getBatchName());
        equalsBuilder.append(jiraTicket, labBatch.getJiraTicket());

        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {

        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();

        hashCodeBuilder.append(isActive).append(batchName).append(jiraTicket);

        return hashCodeBuilder.hashCode();
    }

    /**
     * Helper method to determine if a batch applies to a group of lab vessels
     * <p/>
     * Typically called by looping through the nearest batches of one of the vessels in the vessel group
     *
     * @param targetBatch
     * @param batchSet
     *
     * @return
     */
    public static boolean isCommonBatch(LabBatch targetBatch, Collection<LabVessel> batchSet) {

        boolean result = false;

        if (targetBatch.getStartingBatchLabVessels().containsAll(batchSet)) {
            result = true;
        }

        return result;
    }

    /**
     * Future implementation of this method would get a starting vessel based on its specified position (Lane) defined
     * during the creation of an FCT/MiSeq ticket.
     * <p/>
     * For now (Exome Express Launch) this method will return the one designated vessel for this batch.
     *
     * @param position position (lane) by which the targeted lab vessel is referenced
     *
     * @return Lab Vessel referenced by the given position.
     */
    public LabVessel getStartingVesselByPosition(VesselPosition position) {
        if (labBatchType != LabBatchType.FCT &&
            labBatchType != LabBatchType.MISEQ) {
            throw new RuntimeException("Vessel by Position is only supported for Flowcell Tickets");
        }

        if (startingBatchLabVessels.size() > 1) {
            throw new RuntimeException("more than one starting vessel for a flowcell is not currently supported");
        }

        return startingBatchLabVessels.iterator().next().getLabVessel();
    }

    @Override
    public String toString() {
        return batchName;
    }
}
