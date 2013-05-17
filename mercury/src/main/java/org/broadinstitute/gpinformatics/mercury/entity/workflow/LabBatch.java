package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.*;
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
    @JoinTable(schema = "mercury", name = "lb_starting_lab_vessels", joinColumns = @JoinColumn(name = "lab_batch"),
            inverseJoinColumns = @JoinColumn(name = "starting_lab_vessels"))
    private Set<LabVessel> startingLabVessels = new HashSet<LabVessel>();

    private boolean isActive = true;

    private String batchName;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private JiraTicket jiraTicket;

    // todo jmt get Hibernate to sort this
    @OneToMany(mappedBy = "labBatch")
    private Set<LabEvent> labEvents = new LinkedHashSet<LabEvent>();

    // on the lb_starting_lab_vessels join table
    // fixme reworks need to be included in the startingVessels
    // list because they are starting vessels...but just
    // marked as rework
    @OneToMany(cascade = CascadeType.ALL)
    private Collection<LabVessel> reworks = new HashSet<LabVessel>();

    private Date createdOn;

    private boolean isValidationBatch;

    /**
     * needed for fix-up test
     *
     */
    protected void setLabBatchType(LabBatchType labBatchType) {
        this.labBatchType = labBatchType;
    }

    public enum LabBatchType {
        /** A batch created as part of workflow, e.g. an LCSET */
        WORKFLOW,
        /** A batch created in BSP, typically named BP-1234 */
        BSP,
        /** Receipt of samples into BSP, from external collaborators, typically named SK-1234 for sample kit */
        SAMPLES_RECEIPT,
        /** Import of BSP samples into Mercury */
        SAMPLES_IMPORT
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LabBatchType labBatchType;

    @Transient
    private String batchDescription;

    @Transient
    private Date dueDate;

    @Transient
    private String important;

    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "labBatch")
    private Set<BucketEntry> bucketEntries = new HashSet<BucketEntry>();

    /**
     * Create a new batch with the given name
     * and set of @link Starter starting materials
     *
     * @param starters
     */
    public LabBatch(String batchName, Set<LabVessel> starters, LabBatchType labBatchType) {
        if (batchName == null) {
            throw new NullPointerException("BatchName cannot be null");
        }
        if (starters == null) {
            throw new NullPointerException("starters cannot be null");
        }
        if(labBatchType == null) {
            throw new NullPointerException("labBatchType cannot be null");
        }
        this.batchName = batchName;
        this.labBatchType = labBatchType;
        for (LabVessel starter : starters) {
            addLabVessel(starter);
        }
        createdOn = new Date();
    }

    public LabBatch(String batchName, Set<LabVessel> startingLabVessels, LabBatchType labBatchType,
            String batchDescription, Date dueDate, String important) {

        this(batchName, startingLabVessels, labBatchType);
        this.batchDescription = batchDescription;
        this.dueDate = dueDate;
        this.important = important;
    }


    /**
     * Adds the given rework vessels to the list of reworks for the batch
     * and updates the RapSheet so the samples know they are rework.
     * @param newRework
     */
    public void addReworks(Collection<LabVessel> newRework) {
        reworks.addAll(newRework);
        for (LabVessel vessel : newRework) {
            for (MercurySample sample : vessel.getMercurySamples()) {
                sample.getRapSheet().activateRework();
            }
        }
    }

    public void setReworks(Collection<LabVessel> reworks) {
        this.reworks = reworks;
    }

    public Collection<LabVessel> getReworks() {
        return reworks;
    }

    protected LabBatch() {
    }

    public Set<LabVessel> getStartingLabVessels() {
        return startingLabVessels;
    }

    public List<LabVessel> getStartingLabVesselsList() {
        return new ArrayList<LabVessel>(startingLabVessels);
    }

    public void addLabVessel(@Nonnull LabVessel labVessel) {
        if (labVessel == null) {
            throw new NullPointerException("vessel cannot be null.");
        }
        startingLabVessels.add(labVessel);
        labVessel.addLabBatch(this);
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

    public void setJiraTicket(JiraTicket jiraTicket) {
        this.jiraTicket = jiraTicket;
        jiraTicket.setLabBatch(this);

        batchName = this.jiraTicket.getTicketName();
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
        Map<Date, LabEvent> sortedTreeMap = new TreeMap<Date, LabEvent>();
        for (LabEvent event : getLabEvents()) {
            sortedTreeMap.put(event.getEventDate(), event);
        }
        return new ArrayList<LabEvent>(sortedTreeMap.values());
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
        return isValidationBatch;
    }

    public void setValidationBatch(boolean validationBatch) {
        isValidationBatch = validationBatch;
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
     * Helper nethod to dynamically create batch names based on Input from PDM's.  The format for the Names of the
     * batches, when not manually defined, will be:
     * <p/>
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
    /**
     * RequiredSubmissionFields is an enum intended to assist in the creation of a Jira ticket
     * for Product orders
     */
    public enum RequiredSubmissionFields implements CustomField.SubmissionField {
        PROTOCOL("Protocol", true),

        //Will not have WR ID info in Mercury.  Set to a Blank string
        WORK_REQUEST_IDS("Work Request ID(s)", true),
        POOLING_STATUS("Pooling Status", true),
        PRIORITY("Priority", false),
        DUE_DATE("Due Date", false),

        //User comments at batch creation (Post Dec 1 addition)
        IMPORTANT("Important", true),

        // ??
        NUMBER_OF_CONTROLS("Number of Controls", true),
        NUMBER_OF_SAMPLES("Number of Samples", true),

        //        DO not set this value.  Leave at its default (for now).
        LIBRARY_QC_SEQUENCING_REQUIRED("Library QC Sequencing Required?", true),

        //Radio Button custom field
        PROGRESS_STATUS("Progress Status", true),

        //List of Sample names
        GSSR_IDS("GSSR ID(s)", true),

        LIMS_ACTIVITY_STREAM("LIMS Activity Stream", true);

        private final String fieldName;
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

    public LabVessel[] getStartingVesselsArray() {
        return startingLabVessels.toArray(new LabVessel[startingLabVessels.size()]);
    }

    public Set<BucketEntry> getBucketEntries() {
        return bucketEntries;
    }

    public void addBucketEntry(BucketEntry bucketEntry) {
        bucketEntries.add(bucketEntry);
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
     *
     * Typically called by looping through the nearest batches of one of the vessels in the vessel group
     *
     * @param targetBatch
     * @param batchSet
     * @return
     */
    public static boolean isCommonBatch(LabBatch targetBatch, Collection<LabVessel> batchSet) {

        boolean result = false;

        if (targetBatch.getStartingLabVessels().containsAll(batchSet)) {
            result = true;
        }

        return result;
    }
}
