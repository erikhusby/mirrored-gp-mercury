package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import com.google.common.collect.Sets;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestSessionEjb;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToVesselTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.notice.StatusNote;
import org.broadinstitute.gpinformatics.mercury.entity.notice.UserRemarks;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.sample.TubeTransferException;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Formula;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * This entity represents a piece of plastic or glass that holds sample, reagent or (if it embeds a
 * {@link VesselContainer) other plastic.
 * In-place lab events can apply to any LabVessel, whereas SectionTransfers and CherryPickTransfers apply to
 * LabVessels with a VesselContainer role (racks and plates), and VesselToVessel and VesselToSection transfers
 * apply to containees (tubes and wells).
 */

@Entity
@Audited
@Table(schema = "mercury", uniqueConstraints = @UniqueConstraint(columnNames = {"label"}))
@BatchSize(size = 50)
public abstract class LabVessel implements Serializable {

    private static final long serialVersionUID = 2868707154970743503L;

    private static final Log logger = LogFactory.getLog(LabVessel.class);

    /** Determines whether diagnostics are printed.  This is done as a constant, rather than as a logging level,
     * because the compiler should be smart enough to remove the printing code if the constant is false, whereas
     * a logging level would require frequent checks in heavily used code.
     */
    public static final boolean DIAGNOSTICS = false;
    public static final boolean EVENT_DIAGNOSTICS = false;

    @SequenceGenerator(name = "SEQ_LAB_VESSEL", schema = "mercury", sequenceName = "SEQ_LAB_VESSEL")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LAB_VESSEL")
    @Id
    private Long labVesselId;

    private String label;

    private Date createdOn;

    private BigDecimal volume;

    private BigDecimal concentration;

    private BigDecimal receptacleWeight;

    @OneToMany(cascade = CascadeType.PERSIST) // todo jmt should this have mappedBy?
    @JoinTable(schema = "mercury")
    @BatchSize(size = 100)
    private final Set<JiraTicket> ticketsCreated = new HashSet<>();

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "labVessel")
    @BatchSize(size = 100)
    private Set<LabBatchStartingVessel> labBatches = new HashSet<>();

    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "dilutionVessel")
    private Set<LabBatchStartingVessel> dilutionReferences = new HashSet<>();

    @ManyToMany(cascade = CascadeType.PERSIST, mappedBy = "reworks")
    @BatchSize(size = 100)
    private Set<LabBatch> reworkLabBatches = new HashSet<>();

    // todo jmt separate role for reagents?
    @ManyToMany(cascade = CascadeType.PERSIST)
    // have to specify name, generated aud name is too long for Oracle
    @JoinTable(schema = "mercury", name = "lv_reagent_contents", joinColumns = @JoinColumn(name = "lab_vessel"),
            inverseJoinColumns = @JoinColumn(name = "reagent_contents"))
    @BatchSize(size = 100)
    private Set<Reagent> reagentContents = new HashSet<>();

    /**
     * Counts the number of rows in the many-to-many table.  Reference this count before fetching the collection, to
     * avoid an unnecessary database round trip
     */
    @NotAudited
    @Formula("(select count(*) from lv_reagent_contents where lv_reagent_contents.lab_vessel = lab_vessel_id)")
    private Integer reagentContentsCount = 0;

    // todo jmt separate role for containee?
    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(schema = "mercury")
    @BatchSize(size = 100)
    private Set<LabVessel> containers = new HashSet<>();

    /**
     * Counts the number of rows in the many-to-many table.  Reference this count before fetching the collection, to
     * avoid an unnecessary database round trip
     */
    @NotAudited
    @Formula("(select count(*) from lab_vessel_containers where lab_vessel_containers.lab_vessel = lab_vessel_id)")
    private Integer containersCount = 0;

    /**
     * Reagent additions and machine loaded events, i.e. not transfers
     */
    @OneToMany(mappedBy = "inPlaceLabVessel", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    @BatchSize(size = 100)
    private Set<LabEvent> inPlaceLabEvents = new HashSet<>();

    @OneToMany // todo jmt should this have mappedBy?
    @JoinTable(schema = "mercury")
    private Collection<StatusNote> notes = new HashSet<>();

    @OneToMany(mappedBy = "labVessel", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    @BatchSize(size = 100)
    private Set<BucketEntry> bucketEntries = new HashSet<>();

    /**
     * Counts the number of bucketEntries this vessel is assigned to.
     * Primary use-case to ID samples that have been transferred from collaborator tubes, but not added to a product order.
     */
    @NotAudited
    @Formula("(select count(*) from bucket_entry where bucket_entry.lab_vessel_id = lab_vessel_id)")
    private Integer bucketEntriesCount = 0;

    @Embedded
    private UserRemarks userRemarks;

    // todo jmt separate role for sample holder?
    @ManyToMany(cascade = CascadeType.PERSIST)
    @BatchSize(size = 100)
    private Set<MercurySample> mercurySamples = new HashSet<>();

    // todo jmt set these fields db-free
    @OneToMany(mappedBy = "sourceVessel", cascade = CascadeType.PERSIST)
    @BatchSize(size = 100)
    private Set<VesselToVesselTransfer> vesselToVesselTransfersThisAsSource = new HashSet<>();

    @OneToMany(mappedBy = "targetVessel", cascade = CascadeType.PERSIST)
    @BatchSize(size = 100)
    private Set<VesselToVesselTransfer> vesselToVesselTransfersThisAsTarget = new HashSet<>();

    @OneToMany(mappedBy = "sourceVessel", cascade = CascadeType.PERSIST)
    @BatchSize(size = 100)
    private Set<VesselToSectionTransfer> vesselToSectionTransfersThisAsSource = new HashSet<>();

    @OneToMany(mappedBy = "labVessel", cascade = CascadeType.PERSIST)
    private Set<LabMetric> labMetrics = new HashSet<>();

    @Transient
    private Map<String, Set<LabMetric>> metricMap;

    /**
     * Set by {@link #preProcessEvents()}
     */
    @Transient
    private Set<LabBatch> preProcessedEvents;

    protected LabVessel(String label) {
        createdOn = new Date();
        if (label == null || label.isEmpty() || label.equals("0")) {
            throw new RuntimeException("Invalid label " + label);
        }
        this.label = label;
    }

    protected LabVessel() {
    }

    @SuppressWarnings("unused")
    private static Collection<String> getVesselNameList(Collection<LabVessel> vessels) {

        List<String> vesselNames = new ArrayList<>(vessels.size());

        for (LabVessel currVessel : vessels) {
            vesselNames.add(currVessel.getLabCentricName());
        }

        return vesselNames;
    }

    public boolean isDNA() {
        for (SampleInstance si : getSampleInstances()) {
            if (!si.getStartingSample().getSampleData().getMaterialType().startsWith("DNA:")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Well A01, Lane 3, Region 6 all might
     * be considered a labeled sub-section
     * of a lab vessel.  Labels are GUIDs
     * for LabVessels; no two LabVessels
     * may share this id.  It's primarily the
     * barcode on the piece of plastic.
     */
    public String getLabel() {
        return label;
    }

    /**
     * This is used only for fixups.
     *
     * @param label barcode
     */
    void setLabel(String label) {
        this.label = label;
    }

    public Set<VesselToSectionTransfer> getVesselToSectionTransfersThisAsSource() {
        return vesselToSectionTransfersThisAsSource;
    }

    public void addMetric(LabMetric labMetric) {
        labMetrics.add(labMetric);
        labMetric.setLabVessel(this);
    }

    public Set<LabMetric> getMetrics() {
        return labMetrics;
    }

    public Set<LabMetric> getConcentrationMetrics() {
        if(labMetrics != null) {
            Set<LabMetric> concentrationLabMetrics = new HashSet<>();
            for (LabMetric labMetric: labMetrics) {
                if(labMetric.getName().getCategory() == LabMetric.MetricType.Category.CONCENTRATION) {
                    concentrationLabMetrics.add(labMetric);
                }
            }
            return concentrationLabMetrics;
        }

        return null;
    }

    @SuppressWarnings("unused")
    public Map<String, Set<LabMetric>> getMetricMap() {
        return metricMap;
    }

    @SuppressWarnings("unused")
    public void setMetricMap(Map<String, Set<LabMetric>> metricMap) {
        this.metricMap = metricMap;
    }

    /**
     * Reagent templates, how to register "these 40
     * plates contain adaptors laid out like
     * so:..."
     * <p/>
     * Special subclass for DNAReagent to deal with
     * indexes and adaptors?  Or give Reagent a way
     * to express how it modifies the molecular envelope?
     *
     * @return reagents
     */
    public Set<Reagent> getReagentContents() {
        if (reagentContentsCount != null && reagentContentsCount > 0) {
            return reagentContents;
        }
        return Collections.emptySet();
    }

    public void addReagent(Reagent reagent) {
        reagentContents.add(reagent);
        if (reagentContentsCount == null) {
            reagentContentsCount = 0;
        }
        reagentContentsCount++;
    }

    public void addToContainer(VesselContainer<?> vesselContainer) {
        containers.add(vesselContainer.getEmbedder());
        if (containersCount == null) {
            containersCount = 0;
        }
        containersCount++;
    }

    public Set<VesselContainer<?>> getContainers() {
        Set<VesselContainer<?>> vesselContainers = new HashSet<>();
        if (containersCount != null && containersCount > 0) {
            for (LabVessel container : containers) {
                vesselContainers.add(container.getContainerRole());
            }
        }

        return Collections.unmodifiableSet(vesselContainers);
    }

    // todo notion of a "sample group", not a cohort,
    // but rather an ID for the pool of samples within
    // a container.  useful for finding "related"
    // libraries, related by the group of samples

    /**
     * Get the name of the thing.  This
     * isn't just getName() because that would
     * probably clash with something else.
     * <p/>
     * SGM: 6/15/2012 Update.  Added code to return the
     * <a href="http://en.wikipedia.org/wiki/Base_36#Java_implementation" >Base 36 </a> version of the of the label.
     * This implementation assumes that the label can be converted to a long
     *
     * @return the name for the lab
     */
    @Transient
    public String getLabCentricName() {
        String vesselContentName;

        try {
            vesselContentName = Long.toString(Long.parseLong(label), 36);
        } catch (NumberFormatException nfe) {
            vesselContentName = label;
            if (logger.isDebugEnabled()) {
                logger.debug("Could not return Base 36 version of label. Returning original label instead");
            }
        }

        return vesselContentName;
    }

    /**
     * Get LabEvents that are transfers from this vessel
     *
     * @return transfers
     */
    public Set<LabEvent> getTransfersFrom() {
        Set<LabEvent> transfersFrom = new HashSet<>();
        for (VesselToVesselTransfer vesselToVesselTransfer : vesselToVesselTransfersThisAsSource) {
            transfersFrom.add(vesselToVesselTransfer.getLabEvent());
        }
        if (getContainerRole() == null) {
            for (VesselContainer<?> vesselContainer : getContainers()) {
                transfersFrom.addAll(vesselContainer.getTransfersFrom());
            }
            return transfersFrom;
        } else {
            return getContainerRole().getTransfersFrom();
        }
    }

    /**
     * Get LabEvents that are transfers to this vessel
     *
     * @return transfers
     */
    public Set<LabEvent> getTransfersTo() {
        Set<LabEvent> transfersTo = new HashSet<>();
        for (VesselToVesselTransfer vesselToVesselTransfer : vesselToVesselTransfersThisAsTarget) {
            transfersTo.add(vesselToVesselTransfer.getLabEvent());
        }
        if (getContainerRole() == null) {
            for (VesselContainer<?> vesselContainer : getContainers()) {
                transfersTo.addAll(vesselContainer.getTransfersTo());
            }
        } else {
            transfersTo.addAll(getContainerRole().getTransfersTo());
        }
        return transfersTo;
    }

    /**
     * Get LabEvents that are transfers to this vessel, including re-arrays
     *
     * @return transfers
     */
    public Set<LabEvent> getTransfersToWithReArrays() {
        Set<LabEvent> transfersTo = new HashSet<>();
        for (VesselToVesselTransfer vesselToVesselTransfer : vesselToVesselTransfersThisAsTarget) {
            transfersTo.add(vesselToVesselTransfer.getLabEvent());
        }
        if (getContainerRole() == null) {
            for (VesselContainer<?> vesselContainer : getContainers()) {
                transfersTo.addAll(vesselContainer.getTransfersToWithRearrays());
            }
        } else {
            transfersTo.addAll(getContainerRole().getTransfersToWithRearrays());
        }
        return transfersTo;
    }

    public abstract VesselGeometry getVesselGeometry();

    /**
     * When a {@link org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket} is created for a
     * {@link org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel}, let's
     * remember that fact.  It'll be useful when someone wants
     * to know all the lab work that was done for
     * a StartingSample.
     *
     * @param jiraTicket The jira ticket
     */
    @SuppressWarnings("unused")
    public void addJiraTicket(JiraTicket jiraTicket) {
        if (jiraTicket != null) {
            ticketsCreated.add(jiraTicket);
        }
    }

    /**
     * Get all the {@link JiraTicket jira tickets} that were started
     * with this {@link LabVessel}
     *
     * @return The jira tickets
     */
    @SuppressWarnings("unused")
    public Collection<JiraTicket> getJiraTickets() {
        return ticketsCreated;
    }

    @SuppressWarnings("unused")
    public UserRemarks getUserRemarks() {
        return userRemarks;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public Set<LabEvent> getInPlaceLabEvents() {
        return inPlaceLabEvents;
    }

    public Set<LabEvent> getInPlaceEventsWithContainers() {
        Set<LabEvent> totalInPlaceEventsSet = Collections.unmodifiableSet(inPlaceLabEvents);
        for (LabVessel vesselContainer : containers) {
            totalInPlaceEventsSet = Sets.union(totalInPlaceEventsSet, vesselContainer.getInPlaceEventsWithContainers());
        }
        return totalInPlaceEventsSet;
    }

    private List<LabEvent> getAllEventsSortedByDate() {
        Map<Date, LabEvent> sortedTreeMap = new TreeMap<>();
        for (LabEvent event : getEvents()) {
            sortedTreeMap.put(event.getEventDate(), event);
        }
        return new ArrayList<>(sortedTreeMap.values());
    }

    public void addInPlaceEvent(LabEvent labEvent) {
        inPlaceLabEvents.add(labEvent);
        labEvent.setInPlaceLabVessel(this);
    }

    public abstract ContainerType getType();

    public static Collection<String> extractPdoKeyList(Collection<LabVessel> labVessels) {

        return extractPdoLabVesselMap(labVessels).keySet();
    }

    public static Map<String, Set<LabVessel>> extractPdoLabVesselMap(Collection<LabVessel> labVessels) {

        Map<String, Set<LabVessel>> vesselByPdoMap = new HashMap<>();

        for (LabVessel currVessel : labVessels) {
            Set<SampleInstance> sampleInstances = currVessel.getSampleInstances(SampleType.WITH_PDO, null);

            for (SampleInstance sampleInstance : sampleInstances) {
                String pdoKey = sampleInstance.getProductOrderKey();

                if (!vesselByPdoMap.containsKey(pdoKey)) {
                    vesselByPdoMap.put(pdoKey, new HashSet<LabVessel>());
                }
                vesselByPdoMap.get(pdoKey).add(currVessel);
            }
        }

        return vesselByPdoMap;
    }

    @SuppressWarnings("unused")
    public String getNearestLabBatchesString() {
        Collection<LabBatch> nearest = getNearestLabBatches();
        if (nearest == null) {
            return "";
        }

        return StringUtils.join(nearest, "");
    }

    @SuppressWarnings("unused")
    public int getNearestLabBatchesCount() {
        Collection<LabBatch> nearest = getNearestLabBatches();
        if (nearest == null) {
            return 0;
        }

        return nearest.size();
    }

    /**
     * Sets the rework state to inactive, meaning it will no longer appear in the bucket.
     */
    public void deactivateRework() {
        for (MercurySample sample : getMercurySamples()) {
            sample.getRapSheet().deactivateRework();
        }
    }

    /**
     * This method gets a collection of the nearest lab metrics of the specified type. This only traverses ancestors.
     *
     * @param metricType The type of metric to search for during the traversal.
     *
     * @return A list of the closest metrics of the type specified, ordered by ascending date
     */
    public List<LabMetric> getNearestMetricsOfType(LabMetric.MetricType metricType) {
        if (getContainerRole() != null) {
            return getContainerRole().getNearestMetricOfType(metricType);
        } else {
            TransferTraverserCriteria.NearestLabMetricOfTypeCriteria metricOfTypeCriteria =
                    new TransferTraverserCriteria.NearestLabMetricOfTypeCriteria(metricType);
            evaluateCriteria(metricOfTypeCriteria, TransferTraverserCriteria.TraversalDirection.Ancestors);
            return metricOfTypeCriteria.getNearestMetrics();
        }
    }

    public void addNonReworkLabBatchStartingVessel(LabBatchStartingVessel labBatchStartingVessel) {
        labBatches.add(labBatchStartingVessel);
    }

    public String getLastEventName() {
        String eventName = "";
        List<LabEvent> eventList = new ArrayList<>(getInPlaceAndTransferToEvents());
        Collections.sort(eventList, LabEvent.BY_EVENT_DATE);

        if (!eventList.isEmpty()) {
            eventName = eventList.get(eventList.size() - 1).getLabEventType().getName();
        }
        return eventName;
    }

    /**
     * Check if the vessel has been accessioned
     * Does nothing typically, but will throw an exception if it has been accessioned
     *
     * @throws TubeTransferException if it has been accessioned
     *
     */
    public boolean canBeUsedForAccessioning() {
        return !doesChainOfCustodyInclude(LabEventType.COLLABORATOR_TRANSFER);
    }

    public enum ContainerType {
        STATIC_PLATE("Plate"),
        PLATE_WELL("Plate Well"),
        RACK_OF_TUBES("Tube Rack"),
        TUBE_FORMATION("Tube Formation"),
        TUBE("Tube"),
        FLOWCELL("Flowcell"),
        STRIP_TUBE("Strip Tube"),
        STRIP_TUBE_WELL("Strip Tube Well"),
        PACBIO_PLATE("PacBio Plate"),
        ILLUMINA_RUN_CHAMBER("Illumina Run Chamber"),
        MISEQ_REAGENT_KIT("MiSeq Reagent Kit");

        private String name;

        ContainerType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }


    /**
     * Returned from getAncestors and getDescendants.  Allows code to be shared between evaluateCriteria and
     * getSampleInstances.
     */
    public static class VesselEvent {

        public static final Comparator<VesselEvent> COMPARE_VESSEL_EVENTS_BY_DATE =
                new Comparator<VesselEvent>() {
                    @Override
                    public int compare(VesselEvent o1, VesselEvent o2) {
                        return o1.getLabEvent().getEventDate().compareTo(o2.getLabEvent().getEventDate());
                    }
                };

        private final LabVessel labVessel;
        private final VesselContainer<?> vesselContainer;
        private final VesselPosition position;
        private final LabEvent labEvent;

        public VesselEvent(LabVessel labVessel, VesselContainer<?> vesselContainer, VesselPosition position,
                           LabEvent labEvent) {
            this.labVessel = labVessel;
            this.vesselContainer = vesselContainer;
            this.position = position;
            this.labEvent = labEvent;
        }

        public LabVessel getLabVessel() {
            return labVessel;
        }

        public LabEvent getLabEvent() {
            return labEvent;
        }

        public VesselPosition getPosition() {
            return position;
        }

        public VesselContainer<?> getVesselContainer() {
            return vesselContainer;
        }
    }

    /**
     * Computes the {@link SampleInstance} data
     * on-the-fly by walking the history and applying the
     * StateChange applied during lab work.
     *
     * @return All the sample instances.
     */
    public Set<SampleInstance> getSampleInstances() {
        return getSampleInstances(SampleType.ANY, null);
    }

    /**
     * Type of sample to return.
     */
    public enum SampleType {
        /**
         * Only MercurySamples that have a PDO.
         */
        WITH_PDO,
        /**
         * MercurySamples with PDO, if found, else any.
         */
        PREFER_PDO,
        /**
         * Any MercurySample.
         */
        ANY,
        /**
         * Like PREFER_PDO but traverses to the beginning of the vessel transfer chain to get the "root" sample
         * which appears to be a ProductOrderSample.
         */
        ROOT_SAMPLE
    }

    /**
     * Get the sample instances in this vessel, by traversing the ancestor transfer graph.
     *
     * @param sampleType   whether the sample must have a PDO
     * @param labBatchType the type of lab batch to include in the instance, or null for any
     *
     * @return sample instances
     */
    public Set<SampleInstance> getSampleInstances(SampleType sampleType, @Nullable LabBatch.LabBatchType labBatchType) {
        if (DIAGNOSTICS) {
            System.out.println("getSampleInstances for " + label);
        }
        if (preProcessedEvents == null) {
            preProcessedEvents = preProcessEvents();
        }
        if (getContainerRole() != null) {
            Set<SampleInstance> sampleInstances = getContainerRole().getSampleInstances(sampleType, labBatchType);
            for (SampleInstance sampleInstance : sampleInstances) {
                if (sampleInstance.getLabBatch() == null && preProcessedEvents != null
                    && preProcessedEvents.size() == 1) {
                    sampleInstance.setLabBatch(preProcessedEvents.iterator().next());
                }
            }
            return sampleInstances;
        }
        TraversalResults traversalResults = traverseAncestors(sampleType, labBatchType);
        Set<SampleInstance> filteredSampleInstances;
        if (sampleType == SampleType.WITH_PDO) {
            filteredSampleInstances = new HashSet<>();
            for (SampleInstance sampleInstance : traversalResults.getSampleInstances()) {
                if (sampleInstance.getProductOrderKey() != null) {
                    filteredSampleInstances.add(sampleInstance);
                }
            }
        } else {
            filteredSampleInstances = traversalResults.getSampleInstances();
        }

        // This handles the case where controls are added in a re-array of the destination of the first transfer
        // from LCSET starting tubes.
        if (filteredSampleInstances.size() == 1) {
            SampleInstance sampleInstance = filteredSampleInstances.iterator().next();
            if (sampleInstance.getLabBatch() == null && preProcessedEvents != null && preProcessedEvents.size() == 1) {
                sampleInstance.setLabBatch(preProcessedEvents.iterator().next());
            }
        }
        return filteredSampleInstances;
    }

    public int getSampleInstanceCount() {
        return getSampleInstanceCount(SampleType.ANY, null);
    }

    public int getSampleInstanceCount(SampleType sampleType, @Nullable LabBatch.LabBatchType batchType) {
        return getSampleInstances(sampleType, batchType).size();
    }

    /**
     * The results of traversing (ancestor) vessels.  The main branch in the graph is likely to have (multiple)
     * SampleInstances, while side branches add reagents.
     */
    static class TraversalResults {

        private final Set<SampleInstance> sampleInstances = new HashSet<>();
        private final Set<Reagent> reagents = new HashSet<>();

        void add(TraversalResults traversalResults) {
            sampleInstances.addAll(traversalResults.getSampleInstances());
            reagents.addAll(traversalResults.getReagents());
        }

        public Set<SampleInstance> getSampleInstances() {
            return sampleInstances;
        }

        public Set<Reagent> getReagents() {
            return reagents;
        }

        public void add(SampleInstance sampleInstance) {
            sampleInstances.add(sampleInstance);
        }

        public void add(Reagent reagent) {
            reagents.add(reagent);
        }

        void setBucketEntry(BucketEntry bucketEntry) {
            for (SampleInstance sampleInstance : sampleInstances) {
                sampleInstance.setBucketEntry(bucketEntry);
            }
        }

        void setProductOrderKey(String productOrderKey) {
            for (SampleInstance sampleInstance : sampleInstances) {
                sampleInstance.setProductOrderKey(productOrderKey);
            }
        }

        void setBspExportSample(MercurySample exportSample) {
            for (SampleInstance sampleInstance : sampleInstances) {
                sampleInstance.setBspExportSample(exportSample);
            }
        }

        /**
         * After traversing all ancestors in a level in the hierarchy, apply reagents to that level, if any.
         * Reagents are consumed when they are applied to SampleInstances, they don't continue to be applied to
         * other levels.
         */
        public void completeLevel() {
            if (!sampleInstances.isEmpty()) {
                for (SampleInstance sampleInstance : sampleInstances) {
                    if (!reagents.isEmpty()) {
                        for (Reagent reagent : reagents) {
                            sampleInstance.addReagent(reagent);
                        }
                    }
                    sampleInstance.setEventApplied(false);
                }
                if (!reagents.isEmpty()) {
                    reagents.clear();
                }
            }
        }

        /**
         * Called after an event has been traversed, sets lab batch and product order key.
         *
         * @param labEvent  event that was traversed
         * @param labVessel plastic involved in the event
         */
        public void applyEvent(@Nonnull LabEvent labEvent, @Nonnull LabVessel labVessel) {
            if (EVENT_DIAGNOSTICS) {
                System.out.println("applyEvent " + labEvent.getLabEventType().getName() +
                                   (CollectionUtils.isEmpty(labVessel.getBucketEntries()) ? "" : " with bucket entry"));
            }
            Set<LabBatch> computedLcSets1 = labEvent.getComputedLcSets();
            if (computedLcSets1.size() == 1) {
                LabBatch labBatch = computedLcSets1.iterator().next();
                for (SampleInstance sampleInstance : getSampleInstances()) {
                    if (labEvent.getLabEventType() == LabEventType.POOLING_TRANSFER) {
                        // When setting sample instance bucket entry for Pooling Transfer events, be careful
                        // to apply the bucket entry to only the source vessel's sample instance(s), and not
                        // to the pooled combination of sample instances from other source vessels.
                        if (sampleInstance.isEventApplied()) {
                            continue;
                        }
                        sampleInstance.setEventApplied(true);
                    }
                    int foundBucketEntries = 0;
                    for (BucketEntry bucketEntry : labVessel.getBucketEntries()) {
                        if (bucketEntry.getLabBatch() != null && bucketEntry.getLabBatch().equals(labBatch)) {
                            if (EVENT_DIAGNOSTICS) {
                                System.out.println("SampleInstance " +
                                                   sampleInstance.getStartingSample().getSampleKey() + " gets " +
                                                   bucketEntry.getLabBatch() + " " + bucketEntry.getProductOrder().getBusinessKey() +
                                                   " from " + bucketEntry.getBucket().getBucketDefinitionName() +
                                                   (bucketEntry.getReworkDetail() != null ? " (rework)" : ""));
                            }
                            sampleInstance.setBucketEntry(bucketEntry);
                            foundBucketEntries++;
                        }
                    }
                    if (foundBucketEntries != 1) {
                        if (EVENT_DIAGNOSTICS) {
                            System.out.println("SampleInstance " + sampleInstance.getStartingSample().getSampleKey() +
                                               " gets computed batch " + labBatch);
                        }
                        sampleInstance.setLabBatch(labBatch);
                    }
                }
            }
        }
    }

    /**
     * Traverse all ancestors of this vessel, accumulating SampleInstances.
     *
     * @param sampleType   where to stop recursion
     * @param labBatchType which batches to accumulate
     *
     * @return accumulated sampleInstances
     */
    TraversalResults traverseAncestors(SampleType sampleType, LabBatch.LabBatchType labBatchType) {

        /*
         * Crawl up this vessel's ancestors looking for a vessel that fits the specified sampleType criteria. Once such
         * a vessel is found, we stop crawling start accumulating the results as we crawl back down.
         */
        TraversalResults traversalResults = new TraversalResults();

        boolean continueTraversing = true;
        switch (sampleType) {
        case WITH_PDO:
        case PREFER_PDO:
            if (!bucketEntries.isEmpty() && !mercurySamples.isEmpty()) {
                // Ignore bucket entries that don't have an LCSET yet
                for (BucketEntry bucketEntry : bucketEntries) {
                    if (bucketEntry.getLabBatch() != null) {
                        continueTraversing = false;
                        break;
                    }
                }
            }
            break;
        case ANY:
            if (!mercurySamples.isEmpty()) {
                continueTraversing = false;
            }
            break;
        case ROOT_SAMPLE:
            break;
        }
        if (continueTraversing) {
            List<VesselEvent> vesselEvents = getAncestors();
            for (VesselEvent vesselEvent : vesselEvents) {
                LabVessel labVessel = vesselEvent.getLabVessel();
                if (labVessel == null) {
                    traversalResults.add(vesselEvent.getVesselContainer().traverseAncestors(vesselEvent.getPosition(),
                            sampleType, labBatchType));
                } else {
                    traversalResults.add(labVessel.traverseAncestors(sampleType, labBatchType));
                    traversalResults.applyEvent(vesselEvent.getLabEvent(), labVessel);
                }
            }
        }

        /*
         * Start crawling back down the traversed ancestors. We create SampleInstances for the MercurySamples only from
         * the topmost vessel that actually has MercurySamples. Any other MercurySamples that we encounter on the way
         * back down are not collected because they were already determined to not meet the specified sampleType
         * criteria.
         */

        if (traversalResults.getSampleInstances().isEmpty() && !mercurySamples.isEmpty()) {
            for (MercurySample mercurySample : mercurySamples) {
                SampleInstance sampleInstance = new SampleInstance(mercurySample);
                traversalResults.add(sampleInstance);
            }
        }

        /*
         * LabBatches are accumulated during downward traversal so that we will know all of the batches that the vessel
         * in question is somehow related to.
         */

        for (LabBatch labBatch : getLabBatches()) {
            if (labBatchType == null || labBatch.getLabBatchType() == labBatchType) {
                for (SampleInstance sampleInstance : traversalResults.getSampleInstances()) {
                    sampleInstance.addLabBatches(Collections.singleton(labBatch));
                }
            }
            // If this vessel is a BSP export, sets the aliquot sample.
            // Expects one sample per vessel in the BSP export.
            if (labBatch.getLabBatchType() == LabBatch.LabBatchType.SAMPLES_IMPORT) {
                if (mercurySamples.size() > 1) {
                    throw new RuntimeException("No support for pooled sample imports.");
                }
                traversalResults.setBspExportSample(mercurySamples.iterator().next());
            }
        }

        // FIXME: Ignore rework bucket entries when deciding whether or not there is a single entry.
        // FIXME: There could very well be two bucket entries for different PDOs. We need to decide
        // FIXME: which is the appropriate one in this case (either here or in getSampleInstances).
        if (bucketEntries.size() == 1) {
            BucketEntry bucketEntry = bucketEntries.iterator().next();
            if (bucketEntry.getReworkDetail() == null) {
                traversalResults.setBucketEntry(bucketEntry);
            }
        } else {
            /*
             * Even if there are multiple bucket entries (of any type, rework or not), as long as they all agree on the
             * PDO, we can assume that is the correct PDO.
             */
            Set<String> productOrderKeys = new HashSet<>();
            for (BucketEntry bucketEntry : bucketEntries) {
                productOrderKeys.add(bucketEntry.getProductOrder().getBusinessKey());
            }
            if (productOrderKeys.size() == 1) {
                traversalResults.setProductOrderKey(productOrderKeys.iterator().next());
            }
        }

        // TODO: completeLevel() simply applies reagents to the sample instances, so this code could probably be changed to look more like setting the bspExportSample and bucketEntry above.
        for (Reagent reagent : getReagentContents()) {
            traversalResults.add(reagent);
        }

        traversalResults.completeLevel();
        return traversalResults;
    }

    void traverseDescendants(TransferTraverserCriteria criteria, TransferTraverserCriteria.TraversalDirection direction,
                             int hopCount) {
        for (VesselEvent vesselEvent : getDescendants()) {
            evaluateVesselEvent(criteria, direction, hopCount, vesselEvent);
        }
    }

    /**
     * Get the immediate ancestor vessels to this vessel, in the transfer graph
     *
     * @return ancestors and events
     */
    List<VesselEvent> getAncestors() {
        List<VesselEvent> vesselEvents = new ArrayList<>();
        for (VesselToVesselTransfer vesselToVesselTransfer : vesselToVesselTransfersThisAsTarget) {
            vesselEvents.add(new VesselEvent(vesselToVesselTransfer.getSourceVessel(), null, null,
                    vesselToVesselTransfer.getLabEvent()));
        }
        for (LabVessel container : containers) {
            vesselEvents.addAll(container.getContainerRole().getAncestors(this));
        }
        Collections.sort(vesselEvents, VesselEvent.COMPARE_VESSEL_EVENTS_BY_DATE);
        return vesselEvents;
    }

    /**
     * Get the immediate descendant vessels to this vessel, in the transfer graph
     *
     * @return descendant and events
     */
    private List<VesselEvent> getDescendants() {
        List<VesselEvent> vesselEvents = new ArrayList<>();
        for (VesselToVesselTransfer vesselToVesselTransfer : vesselToVesselTransfersThisAsSource) {
            vesselEvents.add(new VesselEvent(vesselToVesselTransfer.getTargetVessel(), null, null,
                    vesselToVesselTransfer.getLabEvent()));
        }
        for (VesselToSectionTransfer vesselToSectionTransfer : vesselToSectionTransfersThisAsSource) {
            vesselEvents
                    .add(new VesselEvent(vesselToSectionTransfer.getTargetVesselContainer().getEmbedder(), null, null,
                            vesselToSectionTransfer.getLabEvent()));
        }
        for (LabVessel container : containers) {
            vesselEvents.addAll(container.getContainerRole().getDescendants(this));
        }
        Collections.sort(vesselEvents, VesselEvent.COMPARE_VESSEL_EVENTS_BY_DATE);
        return vesselEvents;
    }

    /**
     * Get all events that have happened directly to
     * this vessel.
     *
     * @return in place events, transfers from, transfers to
     */
    public Set<LabEvent> getEvents() {
        return Sets.union(getInPlaceEventsWithContainers(), Sets.union(getTransfersFrom(), getTransfersTo()));
    }

    public Set<LabEvent> getInPlaceAndTransferToEvents() {
        return Sets.union(getInPlaceLabEvents(), getTransfersTo());
    }

    public BigDecimal getVolume() {
        return MathUtils.scaleTwoDecimalPlaces(volume);
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public BigDecimal getConcentration() {
        return concentration;
    }

    public void setConcentration(BigDecimal concentration) {
        this.concentration = concentration;
    }

    public BigDecimal getReceptacleWeight() {
        return receptacleWeight;
    }

    public void setReceptacleWeight(BigDecimal receptacleWeight) {
        this.receptacleWeight = receptacleWeight;
    }

    public Set<BucketEntry> getBucketEntries() {
        return bucketEntries;
    }

    public Integer getBucketEntriesCount(){
        return bucketEntriesCount;
    }

    public void addBucketEntry(BucketEntry bucketEntry) {
        boolean added = bucketEntries.add(bucketEntry);
        if (!added) {
            /*
             * We currently don't have any UI gestures that should allow us to violate the contract for equals/hashCode
             * for BucketEntry (currently using bucket, labVessel, and createdDate). If this exception is ever thrown,
             * it is because we've added such a feature and this should readily be thrown during development and
             * testing. If that happens, BucketEntry will have to be enhanced have more inherent uniqueness, likely
             * using some sort of artificial unique identifier such as a UUID.
             */
            throw new RuntimeException("Vessel already contains an entry equal to: " + bucketEntry);
        }
        clearCaches();
    }

    public void addNonReworkLabBatch(LabBatch labBatch) {
        LabBatchStartingVessel startingVessel = new LabBatchStartingVessel(this, labBatch);
        labBatches.add(startingVessel);
    }

    public void addReworkLabBatch(LabBatch reworkLabBatch) {
        reworkLabBatches.add(reworkLabBatch);
    }

    public Set<LabBatch> getLabBatches() {
        Set<LabBatch> allLabBatches = new HashSet<>();
        for (LabBatchStartingVessel batchStartingVessel : labBatches) {
            allLabBatches.add(batchStartingVessel.getLabBatch());
        }
        allLabBatches.addAll(reworkLabBatches);
        return allLabBatches;
    }

    public Set<LabBatch> getReworkLabBatches() {
        return reworkLabBatches;
    }

    @SuppressWarnings("unused")
    public Set<LabBatchStartingVessel> getLabBatchStartingVessels() {
        return labBatches;
    }

    public List<LabBatchStartingVessel> getLabBatchStartingVesselsByDate() {
        List<LabBatchStartingVessel> batchVesselsByDate = new ArrayList<>(labBatches);
        Collections.sort(batchVesselsByDate, new Comparator<LabBatchStartingVessel>() {
            @Override
            public int compare(LabBatchStartingVessel o1, LabBatchStartingVessel o2) {
                return ObjectUtils.compare(o1.getLabBatch().getCreatedOn(), o2.getLabBatch().getCreatedOn());
            }
        });
        return batchVesselsByDate;
    }

    public Set<LabBatchStartingVessel> getDilutionReferences() {
        return dilutionReferences;
    }

    @SuppressWarnings("unused")
    public void setDilutionReferences(Set<LabBatchStartingVessel> dilutionReferences) {
        this.dilutionReferences = dilutionReferences;
    }

    public void addDilutionReferences(LabBatchStartingVessel dilutionReferences) {
        this.dilutionReferences.add(dilutionReferences);
    }

    /**
     * Get lab batches of the specified type
     *
     * @param labBatchType null to get all types
     *
     * @return filtered lab batches
     *
     * @deprecated this implementation is not necessary with the addition of a method that utilizes transfer entity
     *             traverser
     */
    @Deprecated
    @SuppressWarnings("unused")
    public Collection<LabBatch> getLabBatchesOfType(LabBatch.LabBatchType labBatchType) {
        Collection<LabBatch> allLabBatches = getAllLabBatches(labBatchType);

        if (labBatchType == null) {
            return allLabBatches;
        } else {
            Set<LabBatch> labBatchesOfType = new HashSet<>();
            for (LabBatch labBatch : allLabBatches) {
                if (labBatch.getLabBatchType() == labBatchType) {
                    labBatchesOfType.add(labBatch);
                }
            }
            return labBatchesOfType;
        }
    }

    public Set<MercurySample> getMercurySamples() {
        return mercurySamples;
    }

    public void addSample(MercurySample mercurySample) {
        mercurySamples.add(mercurySample);
    }

    @SuppressWarnings("unused")
    public void addAllSamples(Collection<MercurySample> mercurySamples) {
        this.mercurySamples.addAll(mercurySamples);
    }

    public Long getLabVesselId() {
        return labVesselId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!OrmUtil.proxySafeIsInstance(o, LabVessel.class)) {
            return false;
        }

        LabVessel labVessel = OrmUtil.proxySafeCast(o, LabVessel.class);
        return !(label != null ? !label.equals(labVessel.getLabel()) : labVessel.getLabel() != null);
    }

    @Override
    public int hashCode() {
        return label != null ? label.hashCode() : 0;
    }

    public int compareTo(LabVessel other) {
        CompareToBuilder builder = new CompareToBuilder();

        builder.append(label, other.getLabel());

        return builder.toComparison();
    }

    /**
     * This is over ridden by subclasses that implement {@link VesselContainerEmbedder}
     *
     * @return object representing this vessel's role as a container of other vessels
     */
    public VesselContainer<?> getContainerRole() {
        return null;
    }

    /**
     * Visits nodes in the transfer graph, and applies criteria.
     *
     * @param transferTraverserCriteria object that accumulates results of traversal
     * @param traversalDirection        ancestors or descendants
     */
    public void evaluateCriteria(TransferTraverserCriteria transferTraverserCriteria,
                                 TransferTraverserCriteria.TraversalDirection traversalDirection) {
        evaluateCriteria(transferTraverserCriteria, traversalDirection, null, 0);
    }

    void evaluateCriteria(TransferTraverserCriteria transferTraverserCriteria,
                          TransferTraverserCriteria.TraversalDirection traversalDirection, LabEvent labEvent,
                          int hopCount) {
        TransferTraverserCriteria.Context context =
                new TransferTraverserCriteria.Context(this, labEvent, hopCount, traversalDirection);
        transferTraverserCriteria.evaluateVesselPreOrder(context);
        if (traversalDirection == TransferTraverserCriteria.TraversalDirection.Ancestors) {
            for (VesselEvent vesselEvent : getAncestors()) {
                evaluateVesselEvent(transferTraverserCriteria, traversalDirection, hopCount, vesselEvent);
            }
        } else if (traversalDirection == TransferTraverserCriteria.TraversalDirection.Descendants) {
            traverseDescendants(transferTraverserCriteria, traversalDirection, hopCount);
        } else {
            throw new RuntimeException("Unknown direction " + traversalDirection.name());
        }
        transferTraverserCriteria.evaluateVesselPostOrder(context);
    }

    private static void evaluateVesselEvent(TransferTraverserCriteria transferTraverserCriteria,
                                            TransferTraverserCriteria.TraversalDirection traversalDirection,
                                            int hopCount,
                                            VesselEvent vesselEvent) {
        LabVessel labVessel = vesselEvent.getLabVessel();
        if (labVessel == null) {
            vesselEvent.getVesselContainer().evaluateCriteria(vesselEvent.getPosition(),
                    transferTraverserCriteria, traversalDirection,
                    vesselEvent.getLabEvent(), hopCount + 1);
        } else {
            labVessel.evaluateCriteria(transferTraverserCriteria, traversalDirection, vesselEvent.getLabEvent(),
                    hopCount + 1);
        }
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

    @SuppressWarnings("unused")
    public Collection<String> getNearestProductOrders() {

        if (getContainerRole() != null) {
            return getContainerRole().getNearestProductOrders();
        } else {

            TransferTraverserCriteria.NearestProductOrderCriteria nearestProductOrderCriteria =
                    new TransferTraverserCriteria.NearestProductOrderCriteria();

            evaluateCriteria(nearestProductOrderCriteria, TransferTraverserCriteria.TraversalDirection.Ancestors);
            return nearestProductOrderCriteria.getNearestProductOrders();
        }
    }

    /**
     * Returns the lab batch for a vessel based on the most frequently occurring lab batch in the given container.
     *
     * @param container contains the vessel
     *
     * @return the batch
     */
    @SuppressWarnings("unchecked")
    public LabBatch getPluralityLabBatch(VesselContainer container) {
        Collection<LabBatch> vesselBatches = getAllLabBatches();
        for (LabBatchComposition labBatchComposition : (List<LabBatchComposition>) container.getLabBatchCompositions()) {
            if (vesselBatches.contains(labBatchComposition.getLabBatch())) {
                return labBatchComposition.getLabBatch();
            }
        }
        return null;
    }

    /**
     * Finds all the lab batches represented in this container, and determines how many vessels in this
     * container belong to each of the batches.
     *
     * @return list of lab batches sorted by vessel count (descending).
     */
    public List<LabBatchComposition> getWorkflowLabBatchCompositions() {

        List<SampleInstance> sampleInstances = new ArrayList<>();
        sampleInstances.addAll(getSampleInstances(SampleType.WITH_PDO, null));

        Map<LabBatch, LabBatchComposition> batchMap = new HashMap<>();
        for (SampleInstance sampleInstance : sampleInstances) {
            for (LabBatch labBatch : sampleInstance.getAllWorkflowLabBatches()) {
                LabBatchComposition batchComposition = batchMap.get(labBatch);
                if (batchComposition == null) {
                    batchMap.put(labBatch, new LabBatchComposition(labBatch, 1, sampleInstances.size()));
                } else {
                    batchComposition.addCount();
                }
            }
        }

        List<LabBatchComposition> batchList = new ArrayList<>(batchMap.values());
        Collections.sort(batchList, LabBatchComposition.HIGHEST_COUNT_FIRST);

        return batchList;
    }

    /**
     * Finds all the lab batches represented in this container, and determines how many vessels in this
     * container belong to each of the batches.
     *
     * @return list of lab batches sorted by vessel count (descending).
     */
    @SuppressWarnings("unused")
    public List<LabBatchComposition> getLabBatchCompositions() {

        List<SampleInstance> sampleInstances = new ArrayList<>();
        sampleInstances.addAll(getSampleInstances(SampleType.WITH_PDO, null));

        Map<LabBatch, LabBatchComposition> batchMap = new HashMap<>();
        for (SampleInstance sampleInstance : sampleInstances) {
            for (LabBatch labBatch : sampleInstance.getAllLabBatches()) {
                LabBatchComposition batchComposition = batchMap.get(labBatch);
                if (batchComposition == null) {
                    batchMap.put(labBatch, new LabBatchComposition(labBatch, 1, sampleInstances.size()));
                } else {
                    batchComposition.addCount();
                }
            }
        }

        List<LabBatchComposition> batchList = new ArrayList<>(batchMap.values());
        Collections.sort(batchList, LabBatchComposition.HIGHEST_COUNT_FIRST);

        return batchList;
    }

    public Collection<LabBatch> getAllLabBatches() {
        if (getContainerRole() != null) {
            return getContainerRole().getAllLabBatches();
        } else {
            TransferTraverserCriteria.NearestLabBatchFinder batchCriteria =
                    new TransferTraverserCriteria.NearestLabBatchFinder(null);
            evaluateCriteria(batchCriteria, TransferTraverserCriteria.TraversalDirection.Ancestors);
            return batchCriteria.getAllLabBatches();
        }
    }

    public Collection<LabBatch> getAllLabBatches(LabBatch.LabBatchType type) {
        if (getContainerRole() != null) {
            return getContainerRole().getAllLabBatches(type);
        } else {
            TransferTraverserCriteria.NearestLabBatchFinder batchCriteria =
                    new TransferTraverserCriteria.NearestLabBatchFinder(type);
            evaluateCriteria(batchCriteria, TransferTraverserCriteria.TraversalDirection.Ancestors);
            return batchCriteria.getAllLabBatches();
        }
    }

    public Collection<LabBatch> getNearestLabBatches() {
        if (getContainerRole() != null) {
            return getContainerRole().getNearestLabBatches();
        } else {
            TransferTraverserCriteria.NearestLabBatchFinder batchCriteria =
                    new TransferTraverserCriteria.NearestLabBatchFinder(null);
            evaluateCriteria(batchCriteria, TransferTraverserCriteria.TraversalDirection.Ancestors);
            return batchCriteria.getNearestLabBatches();
        }
    }

    public Collection<LabBatch> getNearestWorkflowLabBatches() {
        if (getContainerRole() != null) {
            return getContainerRole().getNearestLabBatches(LabBatch.LabBatchType.WORKFLOW);
        } else {
            TransferTraverserCriteria.NearestLabBatchFinder batchCriteria =
                    new TransferTraverserCriteria.NearestLabBatchFinder(LabBatch.LabBatchType.WORKFLOW);
            evaluateCriteria(batchCriteria, TransferTraverserCriteria.TraversalDirection.Ancestors);
            return batchCriteria.getNearestLabBatches();
        }
    }

    /**
     * This method iterates over all of the ancestor and descendant vessels, adding them to a map of vessel -> event
     * when the event matches the type given.
     *
     * @param type The type of event to filter the ancestors and descendants by.
     * @param useTargetVessels True if the vessels returned are event targets (vs. sources).
     *
     * @return A map of lab vessels keyed off the event they were present at filtered by type.
     */
    public Map<LabEvent, Set<LabVessel>> findVesselsForLabEventType(LabEventType type, boolean useTargetVessels) {
        List<LabEventType> eventTypeList = new ArrayList<>();
        eventTypeList.add(type);
        return findVesselsForLabEventTypes(eventTypeList, useTargetVessels);
    }

    /**
     * This method iterates over all of the ancestor and descendant vessels, adding them to a map of vessel -> event
     * when the event matches the type given.
     *
     * @param types A list of types of event to filter the ancestors and descendants by.
     * @param useTargetVessels True if the vessels returned are event targets (vs. sources).
     *
     * @return A map of lab vessels keyed off the event they were present at filterd by types.
     */
    public Map<LabEvent, Set<LabVessel>> findVesselsForLabEventTypes(List<LabEventType> types, boolean useTargetVessels) {
        if (getContainerRole() != null) {
            return getContainerRole().getVesselsForLabEventTypes(types);
        }
        TransferTraverserCriteria.VesselForEventTypeCriteria vesselForEventTypeCriteria =
                new TransferTraverserCriteria.VesselForEventTypeCriteria(types, useTargetVessels);
        evaluateCriteria(vesselForEventTypeCriteria, TransferTraverserCriteria.TraversalDirection.Ancestors);
        evaluateCriteria(vesselForEventTypeCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
        return vesselForEventTypeCriteria.getVesselsForLabEventType();
    }

    /**
     * This method walks the vessel transfers in both directions and returns all of the ancestor and descendant
     * vessels.
     *
     * @return A collection containing all ancestor and descendant vessels.
     */
    public Collection<LabVessel> getAncestorAndDescendantVessels() {
        Collection<LabVessel> allVessels;
        allVessels = getAncestorVessels();
        allVessels.addAll(getDescendantVessels());
        return allVessels;
    }

    public Collection<LabVessel> getDescendantVessels() {
        TransferTraverserCriteria.LabVesselDescendantCriteria descendantCriteria =
                new TransferTraverserCriteria.LabVesselDescendantCriteria();
        evaluateCriteria(descendantCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
        return descendantCriteria.getLabVesselDescendants();
    }

    public Collection<LabVessel> getAncestorVessels() {
        TransferTraverserCriteria.LabVesselAncestorCriteria ancestorCritera =
                new TransferTraverserCriteria.LabVesselAncestorCriteria();
        evaluateCriteria(ancestorCritera, TransferTraverserCriteria.TraversalDirection.Ancestors);
        return ancestorCritera.getLabVesselAncestors();
    }

    @SuppressWarnings("unused")
    public Collection<IlluminaFlowcell> getDescendantFlowcells() {
        TransferTraverserCriteria.VesselTypeDescendantCriteria<IlluminaFlowcell> flowcellDescendantCriteria =
                new TransferTraverserCriteria.VesselTypeDescendantCriteria<>(IlluminaFlowcell.class);
        evaluateCriteria(flowcellDescendantCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
        return flowcellDescendantCriteria.getDescendantsOfVesselType();
    }

    /**
     * This method get index information for all samples in this vessel.
     *
     * @return a set of strings representing all indexes in this vessel.
     */
    public List<MolecularIndexReagent> getIndexes() {
        List<MolecularIndexReagent> indexes = new ArrayList<>();
        for (SampleInstance sample : getAllSamples()) {
            for (Reagent reagent : sample.getReagents()) {
                if (OrmUtil.proxySafeIsInstance(reagent, MolecularIndexReagent.class)) {
                    MolecularIndexReagent indexReagent = (MolecularIndexReagent) reagent;
                    indexes.add(indexReagent);
                }
            }
        }

        return indexes;
    }

    /**
     * This method return the unique indexes for this vessel.
     *
     * @return A set of unique indexes in this vessel.
     */
    public Set<MolecularIndexReagent> getUniqueIndexes() {
        return new HashSet<>(getIndexes());
    }

    /**
     * This method gets index information only for the single sample passed in.
     *
     * @param sample The mercury sample to get the index information for.
     *
     * @return A set of indexes for the mercury sample passed in.
     */
    public Set<MolecularIndexReagent> getIndexesForSample(MercurySample sample) {
        Set<MolecularIndexReagent> indexes = new HashSet<>();
        for (SampleInstance sampleInstance : getAllSamplesOfType(SampleType.ANY)) {
            if (sampleInstance.getStartingSample().equals(sample)) {
                indexes.addAll(getIndexesForSampleInstanceV1(sampleInstance));
            }
        }
        return indexes;
    }

    /**
     * This method gets indexes for the single sample instance passed in.
     *
     * @param sampleInstance The sample instance to get the index information for.
     *
     * @return A set of indexes for the sample instance passed in.
     */
    public Set<MolecularIndexReagent> getIndexesForSampleInstanceV1(SampleInstance sampleInstance) {
        return getIndexes(sampleInstance.getReagents());
    }

    /** This method gets indexes for the single sample instance passed in. */
    public Set<MolecularIndexReagent> getIndexesForSampleInstance(SampleInstanceV2 sampleInstance) {
        return getIndexes(sampleInstance.getReagents());
    }

    /** This method gets indexes for the reagents passed in. */
    public Set<MolecularIndexReagent> getIndexes(Collection<Reagent> reagents) {
        Set<MolecularIndexReagent> indexes = new HashSet<>();
        for (Reagent reagent : reagents) {
            if (OrmUtil.proxySafeIsInstance(reagent, MolecularIndexReagent.class)) {
                MolecularIndexReagent indexReagent = (MolecularIndexReagent) reagent;
                indexes.add(indexReagent);
            }
        }
        return indexes;
    }

    @SuppressWarnings("unused")
    public int getIndexesCount() {
        Collection<MolecularIndexReagent> indexes = getIndexes();
        if ((indexes == null) || indexes.isEmpty()) {
            return 0;
        }
        return indexes.size();
    }

    public int getUniqueIndexesCount() {
        Collection<MolecularIndexReagent> indexes = getUniqueIndexes();
        if ((indexes == null) || indexes.isEmpty()) {
            return 0;
        }
        return indexes.size();
    }

    public Set<String> getPdoKeys() {
        Set<String> pdoKeys = new HashSet<>();
        for (SampleInstance sample : getSampleInstances(SampleType.WITH_PDO, null)) {
            String productOrderKey = sample.getProductOrderKey();
            if (productOrderKey != null) {
                pdoKeys.add(productOrderKey);
            }
        }
        return pdoKeys;
    }

    @SuppressWarnings("unused")
    public String getPdoKeysString() {
        Collection<String> keys = getPdoKeys();
        String[] batchArray = keys.toArray(new String[keys.size()]);
        return StringUtils.join(batchArray);
    }

    @SuppressWarnings("unused")
    public int getPdoKeysCount() {
        Collection<String> keys = getPdoKeys();
        if (keys == null) {
            return 0;
        }

        return keys.size();
    }

    /**
     * This method gets all sample instances for a given lab vessel. If this vessel has a container role than the
     * samples are taken from that container.
     *
     * @return a set of sample instances contained in this vessel.
     */
    public Set<SampleInstance> getAllSamples() {
        Set<SampleInstance> allSamples = new HashSet<>();
        allSamples.addAll(getSampleInstances(SampleType.ANY, null));
        if (getContainerRole() != null) {
            allSamples.addAll(getContainerRole().getSampleInstances(SampleType.ANY, null));
        }
        return allSamples;
    }

    public Set<SampleInstance> getAllSamplesOfType(SampleType sampleType) {
        Set<SampleInstance> allSamples = new HashSet<>();
        allSamples.addAll(getSampleInstances(sampleType, null));
        if (getContainerRole() != null) {
            allSamples.addAll(getContainerRole().getSampleInstances(sampleType, null));
        }
        return allSamples;
    }

    /**
     * Goes through all the {@link #getSampleInstances()} and creates
     * a collection of the unique String sample names from {@link MercurySample#getSampleKey()}
     *
     * @return The names
     */
    public Collection<String> getSampleNames() {
        Set<String> sampleNames = new HashSet<>();
        for (SampleInstance sampleInstance : getSampleInstances()) {
            MercurySample sample = sampleInstance.getStartingSample();
            if (sample != null) {
                String sampleKey = StringUtils.trimToNull(sample.getSampleKey());
                if (sampleKey != null) {
                    sampleNames.add(sampleKey);
                }
            }
        }
        return sampleNames;
    }

    public String[] getSampleNamesArray() {
        return getSampleNames().toArray(new String[]{});
    }

    /**
     * Helper method to determine if a given vessel or any of its ancestors are currently in a bucket.
     *
     * @param pdoKey     Business key of a Product order associated with a bucket entry
     * @param bucketName name of a bucket to search for a bucket entry
     *
     * @return boolean indicating whether an ancestor of the current vessel has been in a bucket.
     */
    @SuppressWarnings("unused")
    public boolean isAncestorInBucket(@Nonnull String pdoKey, @Nonnull String bucketName) {

        List<LabVessel> vesselHierarchy = new ArrayList<>();

        vesselHierarchy.add(this);
        vesselHierarchy.addAll(getAncestorVessels());

        for (LabVessel currentAncestor : vesselHierarchy) {
            for (BucketEntry currentEntry : currentAncestor.getBucketEntries()) {
                if (pdoKey.equals(currentEntry.getProductOrder().getBusinessKey()) &&
                    bucketName.equals(currentEntry.getBucket().getBucketDefinitionName()) &&
                    BucketEntry.Status.Active == currentEntry.getStatus()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Helper method to determine if a given vessel is in a bucket.
     *
     *
     * @param productOrder
     * @param bucketName Name of the bucket to search for associations
     * @param compareStatus Status to compare each entry to
     *
     * @return true if the entry is in the bucket with the specified status
     */
    public boolean checkCurrentBucketStatus(
            @Nonnull ProductOrder productOrder, @Nonnull String bucketName, BucketEntry.Status compareStatus) {

        for (BucketEntry currentEntry : getBucketEntries()) {
            if (productOrder.equals(currentEntry.getProductOrder()) &&
                bucketName.equals(currentEntry.getBucket().getBucketDefinitionName()) &&
                compareStatus == currentEntry.getStatus()) {
                return true;
            }
        }

        return false;
    }


    /**
     * Helper method to determine if a given vessel or any of its ancestors have ever been in a bucket.
     *
     * @param bucketName Name of the bucket to search for associations
     *
     * @return true if there is an ancestor in a bucket
     */
    public boolean hasAncestorBeenInBucket(@Nonnull String bucketName) {

        List<LabVessel> vesselHierarchy = new ArrayList<>();

        vesselHierarchy.add(this);
        vesselHierarchy.addAll(this.getAncestorVessels());

        for (LabVessel currentAncestor : vesselHierarchy) {
            for (BucketEntry currentEntry : currentAncestor.getBucketEntries()) {
                if (bucketName.equals(currentEntry.getBucket().getBucketDefinitionName())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * This method gets a map of all of the metrics from this vessel and all ancestor/descendant vessels.
     *
     * @return Returns a map of lab metrics keyed by the metric display name.
     */
    public Map<String, Set<LabMetric>> getMetricsForVesselAndDescendants() {
        Set<LabMetric> allMetrics = new HashSet<>();
        if (metricMap == null) {
            metricMap = new HashMap<>();
            allMetrics.addAll(getMetrics());
            for (LabVessel curVessel : getDescendantVessels()) {
                allMetrics.addAll(curVessel.getMetrics());
            }
            Set<LabMetric> metricSet;
            for (LabMetric metric : allMetrics) {
                metricSet = metricMap.get(metric.getName().getDisplayName());
                if (metricSet == null) {
                    metricSet = new TreeSet<>();
                    metricMap.put(metric.getName().getDisplayName(), metricSet);
                }
                metricSet.add(metric);
            }
        }
        return metricMap;
    }

    /**
     * In preparation for getSampleInstances recursion, sets the computed LCSETs in each ancestor lab event.  This is
     * necessary because a control doesn't have a {@link BucketEntry}; controls become associated with LCSETs by being
     * in the same transfer as vessels with BucketEntries.
     */
    public Set<LabBatch> preProcessEvents() {
        Set<LabEvent> visitedLabEvents = new HashSet<>();
        return recurseEvents(visitedLabEvents, getTransfersToWithReArrays());
    }

    /**
     * Recurses ancestor transfers, setting computed LCSETs.
     *
     * @param visitedLabEvents avoid visiting event twice
     * @param currentTransfers the transfers at the current point in the recursion
     *
     * @return results of recursion
     */
    Set<LabBatch> recurseEvents(Set<LabEvent> visitedLabEvents, Set<LabEvent> currentTransfers) {
        Set<LabBatch> returnLcSets = new HashSet<>();
        for (LabEvent labEvent : currentTransfers) {
            if (visitedLabEvents.add(labEvent)) {
                Set<LabBatch> lcSetsFromRecursion = new HashSet<>();
                for (LabVessel labVessel : labEvent.getSourceLabVessels()) {
                    lcSetsFromRecursion.addAll(recurseEvents(visitedLabEvents, labVessel.getTransfersToWithReArrays()));
                }
                Set<LabBatch> computedLcSets = labEvent.getComputedLcSets();
                if (computedLcSets.isEmpty()) {
                    returnLcSets.addAll(lcSetsFromRecursion);
                    labEvent.addComputedLcSets(lcSetsFromRecursion);
                } else {
                    returnLcSets.addAll(computedLcSets);
                    labEvent.addComputedLcSets(computedLcSets);
                }
            }
        }
        return returnLcSets;
    }

    @Transient
    private Set<SampleInstanceV2> sampleInstances;

    public Set<SampleInstanceV2> getSampleInstancesV2() {
        if (sampleInstances == null) {
            sampleInstances = new LinkedHashSet<>();
            if (getContainerRole() == null) {
                List<VesselEvent> ancestorEvents = getAncestors();
                if (ancestorEvents.isEmpty()) {
                    sampleInstances.add(new SampleInstanceV2(this));
                } else {
                    sampleInstances.addAll(VesselContainer.getAncestorSampleInstances(this, ancestorEvents));
                }
            } else {
                sampleInstances.addAll(getContainerRole().getSampleInstancesV2());
            }
        }
        return sampleInstances;
    }

    /**
     * This is for database-free testing only, when a new transfer makes the caches stale.
     */
    public void clearCaches() {
        sampleInstances = null;
        VesselContainer<?> containerRole = getContainerRole();
        if (containerRole != null) {
            containerRole.clearCaches();
        }
    }

    /** Looks up the most recent lab metric using the lab metric's created date. */
    public LabMetric findMostRecentLabMetric(LabMetric.MetricType metricType) {
        LabMetric latestLabMetric = null;
        for (LabMetric labMetric : getMetrics()) {
            if (labMetric.getName().equals(metricType) &&
                (latestLabMetric == null || latestLabMetric.getCreatedDate() == null ||
                 (labMetric.getCreatedDate() != null &&
                  labMetric.getCreatedDate().after(latestLabMetric.getCreatedDate())))) {
                latestLabMetric = labMetric;
            }
        }
        return latestLabMetric;
    }


    /**
     * Allows the caller to determine if the current vessel or any of its ancestors have been involved in a tube
     * transfer for clinical work.
     *
     * @return true if the vessel is affiliated with clinical work
     */
    public boolean doesChainOfCustodyInclude(LabEventType labEventType) {

        if (LabEvent.isEventPresent(getEvents(), labEventType)) {
            return true;
        }

        TransferTraverserCriteria.LabEventDescendantCriteria eventTraversalCriteria =
                new TransferTraverserCriteria.LabEventDescendantCriteria();
        evaluateCriteria(eventTraversalCriteria, TransferTraverserCriteria.TraversalDirection.Ancestors);

        return LabEvent.isEventPresent(eventTraversalCriteria.getAllEvents(), LabEventType.COLLABORATOR_TRANSFER);
    }

    /**
     * Get metadata values for the given key.
     * @param key e.g. SAMPLE_ID
     * @return array (JSP-friendly)
     */
    public String[] getMetadataValues(Metadata.Key key) {
        List<String> values = new ArrayList<>();
        for (SampleInstanceV2 sampleInstanceV2 : getSampleInstancesV2()) {
            for (MercurySample mercurySample : sampleInstanceV2.getRootMercurySamples()) {
                for (Metadata metadata : mercurySample.getMetadata()) {
                    if (metadata.getKey() == key) {
                        values.add(metadata.getStringValue());
                    }
                }
            }
        }
        return values.toArray(new String[values.size()]);
    }
}
