package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.SampleMetadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToVesselTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.notice.StatusNote;
import org.broadinstitute.gpinformatics.mercury.entity.notice.UserRemarks;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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

    //todo SGM:  create comparator for sorting Containers THEN Create getter that gets sorted containers

    private static final Log logger = LogFactory.getLog(LabVessel.class);

    @SequenceGenerator(name = "SEQ_LAB_VESSEL", schema = "mercury", sequenceName = "SEQ_LAB_VESSEL")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LAB_VESSEL")
    @Id
    private Long labVesselId;

    private String label;

    private Date createdOn;

    private Float volume;

    private Float concentration;

    @OneToMany(cascade = CascadeType.PERSIST) // todo jmt should this have mappedBy?
    @JoinTable(schema = "mercury")
    @BatchSize(size = 100)
    private final Set<JiraTicket> ticketsCreated = new HashSet<>();

    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "labVessel")
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
    @OneToMany(mappedBy = "inPlaceLabVessel", cascade = CascadeType.PERSIST)
    private Set<LabEvent> inPlaceLabEvents = new HashSet<>();

    @OneToMany // todo jmt should this have mappedBy?
    @JoinTable(schema = "mercury")
    private Collection<StatusNote> notes = new HashSet<>();

    @OneToMany(mappedBy = "labVessel", cascade = CascadeType.PERSIST)
    @BatchSize(size = 100)
    private Set<BucketEntry> bucketEntries = new HashSet<>();

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

    @OneToMany(mappedBy = "targetLabVessel", cascade = CascadeType.PERSIST)
    @BatchSize(size = 100)
    private Set<VesselToVesselTransfer> vesselToVesselTransfersThisAsTarget = new HashSet<>();

    @OneToMany(mappedBy = "sourceVessel", cascade = CascadeType.PERSIST)
    @BatchSize(size = 100)
    private Set<VesselToSectionTransfer> vesselToSectionTransfersThisAsSource = new HashSet<>();

    @OneToMany(mappedBy = "labVessel", cascade = CascadeType.PERSIST)
    private Set<LabMetric> labMetrics = new HashSet<>();

    @Transient
    private Integer sampleInstanceCount;

    @Transient
    private Map<String, LabMetric> metricMap;

    /** Set by {@link #preProcessEvents()} */
    @Transient
    private Set<LabBatch> preProcessedEvents;

    protected LabVessel(String label) {
        createdOn = new Date();
        if (label == null || label.equals("0")) {
            throw new RuntimeException("Invalid label " + label);
        }
        this.label = label;
    }

    protected LabVessel() {
    }

    private static Collection<String> getVesselNameList(Collection<LabVessel> vessels) {

        List<String> vesselNames = new ArrayList<>(vessels.size());

        for (LabVessel currVessel : vessels) {
            vesselNames.add(currVessel.getLabCentricName());
        }

        return vesselNames;
    }

    public boolean isDNA() {
        for (SampleInstance si : getSampleInstances()) {
            if (!si.getStartingSample().getBspSampleDTO().getMaterialType().startsWith("DNA:")) {
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
     *
     */
    public String getLabel() {
        return label;
    }

    /**
     * This is used only for fixups.
     *
     * @param label barcode
     */
    public void setLabel(String label) {
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

    public Map<String, LabMetric> getMetricMap() {
        return metricMap;
    }

    public void setMetricMap(Map<String, LabMetric> metricMap) {
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
        if (getReagentContentsCount() != null && getReagentContentsCount() > 0) {
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

    public Integer getReagentContentsCount() {
        return reagentContentsCount;
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
     * @return
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
        if (getContainerRole() == null) {
            Set<LabEvent> transfersFrom = new HashSet<>();
            for (VesselContainer<?> vesselContainer : getContainers()) {
                transfersFrom.addAll(vesselContainer.getTransfersFrom());
            }
            return transfersFrom;
        } else {
            return getContainerRole().getTransfersFrom();
        }
        // todo jmt vessel to vessel transfers
    }

    /**
     * Get LabEvents that are transfers to this vessel
     *
     * @return transfers
     */
    public Set<LabEvent> getTransfersTo() {
        Set<LabEvent> transfersTo = new HashSet<>();
        if (getContainerRole() == null) {
            for (VesselContainer<?> vesselContainer : getContainers()) {
                transfersTo.addAll(vesselContainer.getTransfersTo());
            }
        } else {
            transfersTo.addAll(getContainerRole().getTransfersTo());
        }
        // todo jmt vessel to vessel transfers
        return transfersTo;
    }

    /**
     * Get LabEvents that are transfers to this vessel, including re-arrays
     *
     * @return transfers
     */
    public Set<LabEvent> getTransfersToWithReArrays() {
        Set<LabEvent> transfersTo = new HashSet<>();
        if (getContainerRole() == null) {
            for (VesselContainer<?> vesselContainer : getContainers()) {
                transfersTo.addAll(vesselContainer.getTransfersToWithRearrays());
            }
        } else {
            transfersTo.addAll(getContainerRole().getTransfersToWithRearrays());
        }
        // todo jmt vessel to vessel transfers
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
     * @param jiraTicket
     */
    public void addJiraTicket(JiraTicket jiraTicket) {
        if (jiraTicket != null) {
            ticketsCreated.add(jiraTicket);
        }
    }

    /**
     * Get all the {@link JiraTicket jira tickets} that were started
     * with this {@link LabVessel}
     *
     * @return
     */
    public Collection<JiraTicket> getJiraTickets() {
        return ticketsCreated;
    }

    public UserRemarks getUserRemarks() {
        return userRemarks;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public Set<LabEvent> getInPlaceEvents() {
        Set<LabEvent> totalInPlaceEventsSet = new HashSet<>();
        for (LabVessel vesselContainer : containers) {
            totalInPlaceEventsSet.addAll(vesselContainer.getInPlaceEvents());
        }
        totalInPlaceEventsSet.addAll(inPlaceLabEvents);
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
            Set<SampleInstance> sampleInstances = currVessel.getSampleInstances(LabVessel.SampleType.WITH_PDO, null);

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

    public String getNearestLabBatchesString() {
        Collection<LabBatch> nearest = getNearestLabBatches();
        if ((nearest == null) || nearest.isEmpty()) {
            return "";
        }

        LabBatch[] batchArray = nearest.toArray(new LabBatch[nearest.size()]);
        return StringUtils.join(batchArray);
    }

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
     * @return A collection of the closest metrics of the type specified.
     */
    public Collection<LabMetric> getNearestMetricsOfType(LabMetric.MetricType metricType) {
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
     * Returns a Collection of SampleInstances at given position
     *
     * @param positionName position in vessel, eg: A01
     *
     * @return
     */
    public Collection<SampleInstance> getSamplesAtPosition(@Nonnull String positionName) {
        VesselPosition position = VesselPosition.getByName(positionName);
        return getSamplesAtPosition(position);
    }

    /**
     * Returns a Collection of SampleInstances at given position
     *
     * @param vesselPosition position in vessel, eg: A01
     *
     * @return
     */
    public Collection<SampleInstance> getSamplesAtPosition(@Nonnull VesselPosition vesselPosition) {
        return getSamplesAtPosition(vesselPosition, SampleType.ANY);
    }

    /**
     * Returns a Collection of SampleInstances at given position, traversing until sample type is reached.
     */
    public Set<SampleInstance> getSamplesAtPosition(VesselPosition position, SampleType sampleType) {
        Set<SampleInstance> sampleInstances;
        VesselContainer<?> vesselContainer = getContainerRole();
        if (vesselContainer != null) {
            sampleInstances = vesselContainer.getSampleInstancesAtPosition(position, sampleType);
        } else {
            sampleInstances = getSampleInstances(sampleType, null);
        }
        if (sampleInstances == null) {
            sampleInstances = Collections.emptySet();
        }
        return sampleInstances;
    }


    /**
     * This method gets all of the positions within this vessel that contain the sample instance passed in.
     *
     * @param sampleInstance The sample instance to search for positions of within the vessel
     *
     * @return This returns a list of vessel positions within this vessel that contain the sample instances passed in.
     */
    public List<VesselPosition> getPositionsOfSample(@Nonnull SampleInstance sampleInstance) {
        if (getContainerRole() == null) {
            return Collections.emptyList();
        }

        VesselPosition[] positions = getContainerRole().getEmbedder().getVesselGeometry().getVesselPositions();
        if (positions == null) {
            return Collections.emptyList();
        }

        List<VesselPosition> positionList = new ArrayList<>();
        for (VesselPosition position : positions) {
            for (SampleInstance curSampleInstance : getSamplesAtPosition(position)) {
                if (curSampleInstance.getStartingSample().equals(sampleInstance.getStartingSample())) {
                    positionList.add(position);
                }
            }
        }

        return positionList;
    }

    public Set<VesselPosition> getPositionsOfSample(@Nonnull SampleInstance sampleInstance, SampleType sampleType) {
        if (getContainerRole() == null) {
            return Collections.emptySet();
        }

        VesselPosition[] positions = getContainerRole().getEmbedder().getVesselGeometry().getVesselPositions();
        if (positions == null) {
            return Collections.emptySet();
        }

        Set<VesselPosition> positionList = new HashSet<>();
        for (VesselPosition position : positions) {
            for (SampleInstance curSampleInstance : getSamplesAtPosition(position, sampleType)) {
                if (curSampleInstance.getStartingSample().equals(sampleInstance.getStartingSample())) {
                    positionList.add(position);
                }
            }
        }

        return positionList;
    }

    /**
     * This method will get the last known position of a sample. If the sample is in a tube it will check the last rack
     * it was in. Otherwise it will defer to the getPositionsOfSample.
     *
     * @param sampleInstance The sample instance to find the last position for.
     *
     * @return A list of vessel positions that the sample instance was last known to be at.
     */
    public List<VesselPosition> getLastKnownPositionsOfSample(@Nonnull SampleInstance sampleInstance) {
        if (getContainerRole() == null) {
            Map<Date, List<VesselPosition>> positionList = new TreeMap<>();
            for (VesselContainer<?> container : getContainers()) {
                List<VesselPosition> curPositionLists = positionList.get(container.getEmbedder().getCreatedOn());
                if (curPositionLists == null) {
                    curPositionLists = new ArrayList<>();
                    positionList.put(container.getEmbedder().getCreatedOn(), curPositionLists);
                }
                curPositionLists.add(container.getPositionOfVessel(this));
            }

            if (positionList.isEmpty()) {
                return Collections.emptyList();
            }

            return positionList.get(positionList.keySet().iterator().next());
        }
        return getPositionsOfSample(sampleInstance);
    }

    /**
     * Returned from getAncestors and getDescendants
     */
    public static class VesselEvent {

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
     * @return
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
        ANY
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
        if (preProcessedEvents == null) {
            preProcessedEvents = preProcessEvents();
        }
        if (getContainerRole() != null) {
            Set<SampleInstance> sampleInstances = getContainerRole().getSampleInstances(sampleType, labBatchType);
            for (SampleInstance sampleInstance : sampleInstances) {
                if (sampleInstance.getLabBatch() == null && preProcessedEvents != null && preProcessedEvents.size() == 1) {
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
        if (sampleInstanceCount == null) {
            sampleInstanceCount = getSampleInstances(sampleType, batchType).size();
        }
        return sampleInstanceCount;
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

        /**
         * After traversing all ancestors in a level in the hierarchy, apply reagents to that level, if any.
         * Reagents are consumed when they are applied to SampleInstances, they don't continue to be applied to
         * other levels.
         */
        public void completeLevel() {
            if (!sampleInstances.isEmpty() && !reagents.isEmpty()) {
                for (SampleInstance sampleInstance : sampleInstances) {
                    for (Reagent reagent : reagents) {
                        sampleInstance.addReagent(reagent);
                    }
                }
                reagents.clear();
            }
        }

        /**
         * Called after an event has been traversed, sets lab batch and product order key.
         * @param labEvent event that was traversed
         * @param labVessel plastic involved in the event
         */
        public void applyEvent(@Nonnull LabEvent labEvent, @Nonnull LabVessel labVessel) {
            Set<LabBatch> computedLcSets1 = labEvent.getComputedLcSets();
            if (computedLcSets1.size() == 1) {
                LabBatch labBatch = computedLcSets1.iterator().next();
                for (SampleInstance sampleInstance : getSampleInstances()) {
                    int foundBucketEntries = 0;
                    for (BucketEntry bucketEntry : labVessel.getBucketEntries()) {
                        if (bucketEntry.getLabBatch() != null && bucketEntry.getLabBatch().equals(labBatch)) {
                            sampleInstance.setBucketEntry(bucketEntry);
                            foundBucketEntries++;
                        }
                    }
                    if (foundBucketEntries != 1) {
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
        TraversalResults traversalResults = new TraversalResults();

        boolean continueTraversing = true;
        switch (sampleType) {
        case WITH_PDO:
        case PREFER_PDO:
            if (!bucketEntries.isEmpty() && !mercurySamples.isEmpty()) {
                continueTraversing = false;
            }
            break;
        case ANY:
            if (!mercurySamples.isEmpty()) {
                continueTraversing = false;
            }
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

        if (traversalResults.getSampleInstances().isEmpty() && !mercurySamples.isEmpty()) {
            for (MercurySample mercurySample : mercurySamples) {
                SampleInstance sampleInstance = new SampleInstance(mercurySample);
                traversalResults.add(sampleInstance);
            }
        }
        for (LabBatch labBatch : getLabBatches()) {
            if (labBatchType == null || labBatch.getLabBatchType() == labBatchType) {
                for (SampleInstance sampleInstance : traversalResults.getSampleInstances()) {
                    sampleInstance.addLabBatches(Collections.singleton(labBatch));
                }
            }
            // If this vessel is a BSP export, sets the aliquot sample.
            // Expects one sample per vessel in the BSP export.
            if (labBatch.getLabBatchType() == LabBatch.LabBatchType.SAMPLES_IMPORT) {
                for (MercurySample mercurySample : mercurySamples) {
                    for (SampleInstance sampleInstance : traversalResults.getSampleInstances()) {
                        sampleInstance.setBspExportSample(mercurySample);
                    }
                }
            }
        }

        if (bucketEntries.size() == 1) {
            BucketEntry bucketEntry = bucketEntries.iterator().next();
            if (bucketEntry.getReworkDetail() == null) {
                traversalResults.setBucketEntry(bucketEntry);
            }
        }

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
    private List<VesselEvent> getAncestors() {
        List<VesselEvent> vesselEvents = new ArrayList<>();
        for (VesselToVesselTransfer vesselToVesselTransfer : vesselToVesselTransfersThisAsTarget) {
            vesselEvents.add(new VesselEvent(vesselToVesselTransfer.getSourceVessel(), null, null,
                    vesselToVesselTransfer.getLabEvent()));
        }
        for (LabVessel container : containers) {
            vesselEvents.addAll(container.getContainerRole().getAncestors(this));
        }
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
            vesselEvents.add(new VesselEvent(vesselToVesselTransfer.getTargetLabVessel(), null, null,
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
        return vesselEvents;
    }

    /**
     * Get all events that have happened directly to
     * this vessel.
     *
     * @return in place events, transfers from, transfers to
     */
    public Set<LabEvent> getEvents() {
        Set<LabEvent> events = new HashSet<>();
        events.addAll(getInPlaceEvents());
        events.addAll(getTransfersFrom());
        events.addAll(getTransfersTo());
        return events;
    }

    public Set<LabEvent> getInPlaceAndTransferToEvents() {
        Set<LabEvent> events = new HashSet<>();
        events.addAll(getInPlaceEvents());
        events.addAll(getTransfersTo());
        return events;
    }

    public Float getVolume() {
        return volume;
    }

    public void setVolume(Float volume) {
        this.volume = volume;
    }

    public Float getConcentration() {
        return concentration;
    }

    public void setConcentration(Float concentration) {
        this.concentration = concentration;
    }

    public Set<BucketEntry> getBucketEntries() {
        return Collections.unmodifiableSet(bucketEntries);
    }

    public void addBucketEntry(BucketEntry bucketEntry) {
        bucketEntries.add(bucketEntry);
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

    public Set<LabBatchStartingVessel> getLabBatchStartingVessels() {
        return labBatches;
    }


    public Set<LabBatchStartingVessel> getDilutionReferences() {
        return dilutionReferences;
    }

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
     * traverser
     */
    @Deprecated
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

    /**
     * What {@link SampleMetadata samples} are contained in
     * this container?  Implementations are expected to
     * walk the transfer graph back to a point where
     * they can lookup {@link SampleMetadata} from
     * an external source like BSP or a spreadsheet
     * uploaded for "walk up" sequencing.
     *
     * @return
     */
    public Set<MercurySample> getMercurySamples() {
        Set<MercurySample> foundSamples = new HashSet<>();
        if (!mercurySamples.isEmpty()) {
            foundSamples.addAll(mercurySamples);
        }

        return foundSamples;
    }

    /**
     * For vessels that have been pushed over from BSP, we set
     * the list of samples.  Otherwise, the list of samples
     * is empty and is derived from a walk through event history.
     *
     * @param mercurySample
     */
    public void addSample(MercurySample mercurySample) {
        mercurySamples.add(mercurySample);
    }

    public void addAllSamples(Set<MercurySample> mercurySamples) {
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

        if (label != null ? !label.equals(labVessel.getLabel()) : labVessel.getLabel() != null) {
            return false;
        }

        return true;
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

    // todo jmt unused?
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
    public LabBatch getPluralityLabBatch(VesselContainer container) {
        Collection<LabBatch> vesselBatches = getAllLabBatches();
        for (LabBatchComposition labBatchComposition : (List<LabBatchComposition>) container
                .getLabBatchCompositions()) {
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
     *
     * @return A map of lab vessels keyed off the event they were present at filtered by type.
     */
    public Map<LabEvent, Set<LabVessel>> findVesselsForLabEventType(LabEventType type) {
        List<LabEventType> eventTypeList = new ArrayList<>();
        eventTypeList.add(type);
        return findVesselsForLabEventTypes(eventTypeList);
    }

    /**
     * This method iterates over all of the ancestor and descendant vessels, adding them to a map of vessel -> event
     * when the event matches the type given.
     *
     * @param types A list of types of event to filter the ancestors and descendants by.
     *
     * @return A map of lab vessels keyed off the event they were present at filterd by types.
     */
    public Map<LabEvent, Set<LabVessel>> findVesselsForLabEventTypes(List<LabEventType> types) {
        if (getContainerRole() != null) {
            return getContainerRole().getVesselsForLabEventTypes(types);
        }
        TransferTraverserCriteria.VesselForEventTypeCriteria vesselForEventTypeCriteria =
                new TransferTraverserCriteria.VesselForEventTypeCriteria(types);
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
                indexes.addAll(getIndexesForSampleInstance(sampleInstance));
            }
        }
        return indexes;
    }

    /**
     * This method gets index information only for the single sample instance passed in.
     *
     * @param sampleInstance The sample instance to get the index information for.
     *
     * @return A set of indexes for the sample instance passed in.
     */
    public Set<MolecularIndexReagent> getIndexesForSampleInstance(SampleInstance sampleInstance) {
        Set<MolecularIndexReagent> indexes = new HashSet<>();
        for (Reagent reagent : sampleInstance.getReagents()) {
            if (OrmUtil.proxySafeIsInstance(reagent, MolecularIndexReagent.class)) {
                MolecularIndexReagent indexReagent = (MolecularIndexReagent) reagent;
                indexes.add(indexReagent);
            }
        }
        return indexes;
    }

    /**
     * This method gets a string concatenated representation of all the indexes for a given sample instance.
     *
     * @param sampleInstance Gets indexes for this sample instance, or if null, for all sample instances in the vessel.
     *
     * @return A string containing information about all the indexes.
     */
    public String getIndexesString(SampleInstance sampleInstance) {
        Collection<MolecularIndexReagent> indexes =
                (sampleInstance == null ? getIndexes() : getIndexesForSampleInstance(sampleInstance));

        if ((indexes == null) || indexes.isEmpty()) {
            return "";
        }
        StringBuilder indexInfo = new StringBuilder();
        for (MolecularIndexReagent indexReagent : indexes) {
            indexInfo.append(indexReagent.getMolecularIndexingScheme().getName());
            indexInfo.append(" - ");
            for (MolecularIndexingScheme.IndexPosition hint : indexReagent.getMolecularIndexingScheme()
                    .getIndexes().keySet()) {
                MolecularIndex index = indexReagent.getMolecularIndexingScheme().getIndexes().get(hint);
                indexInfo.append(index.getSequence());
                indexInfo.append("\n");
            }
        }
        return indexInfo.toString();
    }

    /**
     * This method gets a string concatenated representation of all the indexes.
     *
     * @return A string containing information about all the indexes.
     */
    public String getIndexesString() {
        return getIndexesString(null);
    }

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

    public String getPdoKeysString() {
        Collection<String> keys = getPdoKeys();
        String[] batchArray = keys.toArray(new String[keys.size()]);
        return StringUtils.join(batchArray);
    }

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
     * a collection of the unique String sample names from {@link org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample#getSampleKey()}
     *
     * @return
     */
    public Collection<String> getSampleNames() {
        List<String> sampleNames = new ArrayList<>();
        for (SampleInstance sampleInstance : getSampleInstances()) {
            if (sampleInstance.getStartingSample() != null) {
                String sampleKey = sampleInstance.getStartingSample().getSampleKey();
                if (sampleKey != null) {
                    sampleKey = sampleKey.trim();
                    if (!sampleKey.isEmpty()) {
                        sampleNames.add(sampleKey);
                    }
                }
            }
        }
        return sampleNames;
    }


    /**
     * Helper method to determine if a given vessel or any of its ancestors are currently in a bucket.
     *
     * @param pdoKey     Business key of a Product order associated with a bucket entry
     * @param bucketName name of a bucket to search for a bucket entry
     *
     * @return boolean indicating whether an ancestor of the current vessel has been in a bucket.
     */
    public boolean isAncestorInBucket(@Nonnull String pdoKey, @Nonnull String bucketName) {

        List<LabVessel> vesselHierarchy = new ArrayList<>();

        vesselHierarchy.add(this);
        vesselHierarchy.addAll(getAncestorVessels());

        for (LabVessel currentAncestor : vesselHierarchy) {
            for (BucketEntry currentEntry : currentAncestor.getBucketEntries()) {
                if (pdoKey.equals(currentEntry.getPoBusinessKey()) &&
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
     * @param pdoKey     PDO Key with which a vessel may be associated in a bucket
     * @param bucketName Name of the bucket to search for associations
     * @param active
     *
     * @return
     */
    public boolean checkCurrentBucketStatus(@Nonnull String pdoKey, @Nonnull String bucketName,
                                            BucketEntry.Status active) {

        for (BucketEntry currentEntry : getBucketEntries()) {
            if (pdoKey.equals(currentEntry.getPoBusinessKey()) &&
                bucketName.equals(currentEntry.getBucket().getBucketDefinitionName()) &&
                active == currentEntry.getStatus()) {
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
     * @return
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
     * TODO jac should this be a traversal criteria?
     *
     * @return Returns a map of lab metrics keyed by the metric display name.
     */
    public Map<String, LabMetric> getMetricsForVesselAndDescendants() {
        Set<LabMetric> allMetrics = new HashSet<>();
        if (metricMap == null) {
            metricMap = new HashMap<>();
            allMetrics.addAll(getMetrics());
            for (LabVessel curVessel : getAncestorAndDescendantVessels()) {
                allMetrics.addAll(curVessel.getMetrics());
            }
            for (LabMetric metric : allMetrics) {
                metricMap.put(metric.getName().getDisplayName(), metric);
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
     * @param visitedLabEvents avoid visiting event twice
     * @param currentTransfers the transfers at the current point in the recursion
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
                Set<LabBatch> computedLcSets = labEvent.computeLcSets();
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
}
