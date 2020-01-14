package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
@Table(schema = "mercury", uniqueConstraints = @UniqueConstraint(columnNames = {"batch_name"}))
public class LabBatch {

    public static class VesselToLanesInfo {
        private List<VesselPosition> lanes;
        private BigDecimal concentration;
        private LabVessel labVessel;
        private String linkedLcset;
        private String productNames;
        private List<FlowcellDesignation> designations;

        public VesselToLanesInfo(List<VesselPosition> lanes, BigDecimal concentration, LabVessel labVessel,
                String lcsetName, String productName, @Nonnull List<FlowcellDesignation> designations) {
            this.lanes = lanes;
            this.concentration = concentration;
            this.labVessel = labVessel;
            this.linkedLcset = lcsetName;
            this.productNames = productName;
            this.designations = designations;
        }

        public List<VesselPosition> getLanes() {
            return lanes;
        }

        public void setLanes(List<VesselPosition> lanes) {
            this.lanes = lanes;
        }

        public BigDecimal getConcentration() {
            return concentration;
        }

        public void setConcentration(BigDecimal concentration) {
            this.concentration = concentration;
        }

        public LabVessel getLabVessel() {
            return labVessel;
        }

        public void setLabVessel(LabVessel labVessel) {
            this.labVessel = labVessel;
        }

        public String getLinkedLcset() {
            return linkedLcset;
        }

        public void setLinkedLcset(String linkedLcset) {
            this.linkedLcset = linkedLcset;
        }

        public String getProductNames() {
            return productNames;
        }

        public void setProductNames(String productNames) {
            this.productNames = productNames;
        }

        public List<FlowcellDesignation> getDesignations() {
            return designations;
        }

        public void setDesignations(@Nonnull List<FlowcellDesignation> designations) {
            this.designations = designations;
        }
    }

    /**
     * To support lab batch sorting by creation date, newest first
     */
    public static final Comparator<LabBatch> byDateDesc = new Comparator<LabBatch>() {
        @Override
        public int compare(LabBatch labBatchPrime, LabBatch labBatchSecond) {
            return ObjectUtils.compare(labBatchSecond.getCreatedOn(), labBatchPrime.getCreatedOn());
        }
    };

    @Id
    @SequenceGenerator(name = "SEQ_LAB_BATCH", schema = "mercury", sequenceName = "SEQ_LAB_BATCH")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LAB_BATCH")
    private Long labBatchId;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "labBatch")
    private Set<LabBatchStartingVessel> startingBatchLabVessels = new HashSet<>();

    /**
     * Deprecated in favor of LabBatchStartingVessel
     * @see org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch#startingBatchLabVessels
     */
    @Deprecated
    @ManyToMany(cascade = CascadeType.PERSIST)
    // have to specify name, generated aud name is too long for Oracle
    @JoinTable(schema = "MERCURY", name = "LB_STARTING_LAB_VESSELS"
            , joinColumns = {@JoinColumn(name = "LAB_BATCH")}
            , inverseJoinColumns = {@JoinColumn(name = "STARTING_LAB_VESSELS")})
    private Set<LabVessel> startingLabVessels = new HashSet<>();

    private boolean isActive = true;

    private String batchName;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "JIRA_TICKET")
    private JiraTicket jiraTicket;

    // todo jmt get Hibernate to sort this
    @OneToMany(mappedBy = "labBatch", cascade = CascadeType.PERSIST)
    private Set<LabEvent> labEvents = new LinkedHashSet<>();

    /**
     * Vessels in the batch that were added as rework from a previous batch.
     */
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(schema = "MERCURY", name = "LAB_BATCH_REWORKS"
            , joinColumns = @JoinColumn(name = "LAB_BATCH")
            , inverseJoinColumns = @JoinColumn(name = "REWORKS"))
    private Collection<LabVessel> reworks = new HashSet<>();

    private Date createdOn; // todo jmt, should be CREATED_DATE?

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
        MISEQ,
        /**
         * Sample Retrieval and Storage Batch
         */
        SRS
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

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy = "labBatch", orphanRemoval = true)
    private Set<BucketEntry> bucketEntries = new HashSet<>();

    protected LabBatch() {
    }

    /** Not for creating an FCT that has per-lane vessel info. */
    public LabBatch(@Nonnull String batchName, @Nonnull Set<LabVessel> starterVessels,
                    @Nonnull LabBatchType labBatchType) {
        this.batchName = batchName;
        this.labBatchType = labBatchType;
        for (LabVessel starter : starterVessels) {
            addLabVessel(starter);
        }
        createdOn = new Date();
    }

    public LabBatch(@Nonnull String batchName, @Nonnull Set<LabVessel> starterVessels,
                    @Nonnull LabBatchType labBatchType, @Nonnull Date date) {
        this(batchName, starterVessels, labBatchType);
        createdOn = date;
    }

    /** Specialized FCT or MISEQ constructor for test purposes. Puts the starter vessel on all flowcell lanes. */
    public LabBatch(@Nonnull String batchName, @Nonnull LabBatchType labBatchType,
                    IlluminaFlowcell.FlowcellType flowcellType, @Nonnull LabVessel starterVessel,
                    @Nullable BigDecimal concentration) {
        this(batchName, Collections.singletonList(
                new VesselToLanesInfo(Arrays.asList(flowcellType.getVesselGeometry().getVesselPositions()),
                        concentration, starterVessel, null, null, Collections.<FlowcellDesignation>emptyList())),
                labBatchType, flowcellType);
    }

    /** Constructor for FCT or MISEQ where each vessel must be designated to specific lanes. */
    public LabBatch(@Nonnull String batchName, @Nonnull List<VesselToLanesInfo> vesselToLanesInfos,
                    @Nonnull LabBatchType labBatchType,
                    @Nullable IlluminaFlowcell.FlowcellType flowcellType) {
        this.batchName = batchName;
        this.labBatchType = labBatchType;
        this.flowcellType = flowcellType;
        for (VesselToLanesInfo vesselToLanesInfo : vesselToLanesInfos) {
            addLabVessel(vesselToLanesInfo);
        }
        createdOn = new Date();
    }

    public LabBatch(@Nonnull String batchName, @Nonnull Set<LabVessel> startingLabVessels,
                    Set<LabVessel> reworkLabVessels, @Nonnull LabBatchType labBatchType, String workflowName,
                    String batchDescription, Date dueDate, String important) {
        this(batchName, startingLabVessels, labBatchType);
        this.batchDescription = batchDescription;
        this.dueDate = dueDate;
        this.important = important;
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
        LabBatchStartingVessel labBatchStartingVessel = new LabBatchStartingVessel(labVessel, this);
        startingBatchLabVessels.add(labBatchStartingVessel);
        labVessel.addNonReworkLabBatchStartingVessel(labBatchStartingVessel);
    }

    /**
     * Adds a vessel with lane loading info to this FCT/MISEQ batch.
     * @param vesselToLanesInfo holds the flowcell loading parameters.
     */
    public void addLabVessel(@Nonnull VesselToLanesInfo vesselToLanesInfo) {
        for (int i = 0; i < vesselToLanesInfo.getLanes().size(); ++i) {
            FlowcellDesignation designation = (vesselToLanesInfo.getDesignations().size() > i) ?
                    vesselToLanesInfo.getDesignations().get(i) : null;
            LabBatchStartingVessel labBatchStartingVessel =
                    new LabBatchStartingVessel(vesselToLanesInfo.getLabVessel(), this,
                            vesselToLanesInfo.getConcentration(),
                            vesselToLanesInfo.getLanes().get(i), vesselToLanesInfo.getLinkedLcset(),
                            vesselToLanesInfo.getProductNames(), designation);
            startingBatchLabVessels.add(labBatchStartingVessel);
            vesselToLanesInfo.getLabVessel().addNonReworkLabBatchStartingVessel(labBatchStartingVessel);
        }
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

    /**
     * Deprecated in favor of:
     * @see org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch#getStartingBatchLabVessels()
     * @see org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch#getNonReworkStartingLabVessels()
     * @see org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch#getLabBatchStartingVessels()
     */
    @Deprecated
    public Set<LabVessel> getStartingLabVessels() {
        return startingLabVessels;
    }

    /**
     * Deprecated in favor of:
     * @see org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch#addLabVessel(LabVessel)
     * @see org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch#addLabVessels(Collection<LabVessel>)
     * @see org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch#addLabVessel(VesselToLanesInfo)
     */
    @Deprecated
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

    public void setWorkflow(@Nonnull String workflow) {
        workflowName = workflow;
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
    public static String generateBatchName(@Nonnull String workflow, @Nonnull Collection<String> pdoNames) {

        StringBuilder batchName = new StringBuilder();

        batchName.append(workflow).append(": ");
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
     * TicketFields is an enum representing the fields in a Jira ticket.
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
        CLUSTER_STATION("Cluster Station", true),
        MATERIAL_TYPE("Material Type", true),
        LANE_INFO("Lane Info", true),
        READ_STRUCTURE("Read Structure", true),
        SAMPLES_ON_RISK("Samples On Risk", true),
        RISK_CATEGORIZED_SAMPLES("Risk Categorized Samples", true),
        REWORK_SAMPLES("Rework Samples",true),
        PLATE_MAP_UDS("Plate Map UDS",true),

        ISSUE_TYPE_MAP("Issue Type", false),
        ISSUE_TYPE_NAME("name", false),

        // ARRAY tickets
        NUMBER_OF_EMPTIES("Number of Empties", true),
        NUMBER_OF_WELLS("Total Samples, Controls, and Empties", true),
        CONTAINER_ID("Container ID", true),

        // SRS tickets
        //(Sample IDs, Number of Samples defined above)
        SOURCE("Source", true),
        DESTINATION("Destination", true);

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

    public void removeBucketEntry(BucketEntry bucketEntry) {
        bucketEntries.remove(bucketEntry);
        bucketEntry.setLabBatch(null);
        bucketEntry.setStatus(BucketEntry.Status.Active);
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
     * Returns the starting vessel designated for the specified flowcell lane.
     * THIS METHOD IS UNRELIABLE AFTER THE FLOWCELL TRANSFER since the starting
     * vessel may actually have been put on a different lane.
     *
     * @param position the lane to examine.
     * @return the lab vessel that was designated to be put at the given position.
     */
    public LabVessel getStartingVesselByPosition(VesselPosition position) {
        if (labBatchType != LabBatchType.FCT &&
            labBatchType != LabBatchType.MISEQ) {
            throw new RuntimeException("Vessel by Position is only supported for Flowcell Tickets");
        }
        if (startingBatchLabVessels.size() == 1) {
            return startingBatchLabVessels.iterator().next().getLabVessel();
        }
        for (LabBatchStartingVessel labBatchStartingVessel : startingBatchLabVessels) {
            if (labBatchStartingVessel.getVesselPosition() == position) {
                return labBatchStartingVessel.getLabVessel();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return batchName;
    }
}
