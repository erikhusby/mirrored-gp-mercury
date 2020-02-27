package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.search.LabVesselSearchDefinition;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
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
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntity;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.sample.TubeTransferException;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
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
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
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
@BatchSize(size = 20)
public abstract class LabVessel implements Serializable {

    private static final long serialVersionUID = 2868707154970743503L;

    private static final Log logger = LogFactory.getLog(LabVessel.class);

    /**
     * Determines whether diagnostics are printed.  This is done as a constant, rather than as a logging level,
     * because the compiler should be smart enough to remove the printing code if the constant is false, whereas
     * a logging level would require frequent checks in heavily used code.
     */
    public static final boolean DIAGNOSTICS = false;
    public static final boolean EVENT_DIAGNOSTICS = false;

    @SequenceGenerator(name = "SEQ_LAB_VESSEL", schema = "mercury", sequenceName = "SEQ_LAB_VESSEL")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LAB_VESSEL")
    @Id
    private Long labVesselId;

    /**
     * Typically a barcode.
     */
    private String label;

    /**
     * A human readable name e.g. BSP plate name.
     */
    protected String name;

    private Date createdOn;

    private BigDecimal volume;

    private BigDecimal concentration;

    private BigDecimal receptacleWeight;

    private BigDecimal mass;

    @OneToMany(cascade = CascadeType.PERSIST) // todo jmt should this have mappedBy?
    @JoinTable(schema = "mercury", name="LAB_VESSEL_TICKETS_CREATED"
            , joinColumns = {@JoinColumn(name = "LAB_VESSEL")}
            , inverseJoinColumns = {@JoinColumn(name = "TICKETS_CREATED")})
    @BatchSize(size = 20)
    private final Set<JiraTicket> ticketsCreated = new HashSet<>();

    /**
     * Counts the number of rows in the to-many table.  Reference this count before fetching the collection, to
     * avoid an unnecessary database round trip
     */
    @NotAudited
    @Formula("(select count(*) from LAB_VESSEL_TICKETS_CREATED where LAB_VESSEL_TICKETS_CREATED.LAB_VESSEL = lab_vessel_id)")
    private Integer ticketsCreatedCount = 0;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "labVessel")
    @BatchSize(size = 20)
    private Set<LabBatchStartingVessel> labBatches = new HashSet<>();

    /**
     * Counts the number of rows in the to-many table.  Reference this count before fetching the collection, to
     * avoid an unnecessary database round trip
     */
    @NotAudited
    @Formula("(select count(*) from batch_starting_vessels where batch_starting_vessels.LAB_VESSEL = lab_vessel_id)")
    private Integer labBatchesCount = 0;

    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "dilutionVessel")
    private Set<LabBatchStartingVessel> dilutionReferences = new HashSet<>();

    @ManyToMany(cascade = CascadeType.PERSIST, mappedBy = "reworks")
    @BatchSize(size = 20)
    private Set<LabBatch> reworkLabBatches = new HashSet<>();

    /**
     * Counts the number of rows in the many-to-many table.  Reference this count before fetching the collection, to
     * avoid an unnecessary database round trip
     */
    @NotAudited
    @Formula("(select count(*) from LAB_BATCH_REWORKS where LAB_BATCH_REWORKS.REWORKS = lab_vessel_id)")
    private Integer reworkLabBatchesCount = 0;

    // todo jmt separate role for reagents?
    @ManyToMany(cascade = CascadeType.PERSIST)
    // have to specify name, generated aud name is too long for Oracle
    @JoinTable(schema = "mercury", name = "lv_reagent_contents", joinColumns = @JoinColumn(name = "lab_vessel"),
            inverseJoinColumns = @JoinColumn(name = "reagent_contents"))
    @BatchSize(size = 20)
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
    @JoinTable(schema = "mercury", name="LAB_VESSEL_CONTAINERS"
        , joinColumns = {@JoinColumn(name = "LAB_VESSEL")}
        , inverseJoinColumns = {@JoinColumn(name = "CONTAINERS")})
    @BatchSize(size = 20)
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
    @BatchSize(size = 20)
    private Set<LabEvent> inPlaceLabEvents = new HashSet<>();

    /**
     * Counts the number of rows in the many-to-many table.  Reference this count before fetching the collection, to
     * avoid an unnecessary database round trip
     */
    @NotAudited
    @Formula("(select count(*) from lab_event where lab_event.in_place_lab_vessel = lab_vessel_id)")
    private Integer inPlaceEventsCount = 0;

    @OneToMany(mappedBy = "labVessel", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    @BatchSize(size = 20)
    private Set<AbandonVessel> abandonVessels = new HashSet<>();

    @OneToMany(mappedBy = "labVessel", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    @BatchSize(size = 100)
    private Set<SampleInstanceEntity> sampleInstanceEntities = new HashSet<>();

    /**
     * Counts the number of rows in the many-to-many table.  Reference this count before fetching the collection, to
     * avoid an unnecessary database round trip
     */
    @NotAudited
    @Formula("(select count(*) from sample_instance_entity where sample_instance_entity.LAB_VESSEL = lab_vessel_id)")
    private Integer sampleInstanceEntitiesCount = 0;

    @OneToMany // todo jmt should this have mappedBy?
    @JoinTable(schema = "mercury", name = "LAB_VESSEL_NOTES"
            , joinColumns = {@JoinColumn(name = "LAB_VESSEL")}
            , inverseJoinColumns = {@JoinColumn(name = "NOTES")})
    private Collection<StatusNote> notes = new HashSet<>();

    @OneToMany(mappedBy = "labVessel", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    @BatchSize(size = 20)
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
    @JoinTable(schema = "mercury", name = "LAB_VESSEL_MERCURY_SAMPLES"
            , joinColumns = {@JoinColumn(name = "LAB_VESSEL")}
            , inverseJoinColumns = {@JoinColumn(name = "MERCURY_SAMPLES")})
    @BatchSize(size = 20)
    private Set<MercurySample> mercurySamples = new HashSet<>();

    /**
     * Counts the number of rows in the many-to-many table.  Reference this count before fetching the collection, to
     * avoid an unnecessary database round trip
     */
    @NotAudited
    @Formula("(select count(*) from LAB_VESSEL_MERCURY_SAMPLES where LAB_VESSEL_MERCURY_SAMPLES.LAB_VESSEL = lab_vessel_id)")
    private Integer mercurySamplesCount = 0;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="STORAGE_LOCATION")
    private StorageLocation storageLocation;

    @ManyToMany(cascade = CascadeType.PERSIST, mappedBy = "labVessels")
    private Set<State> states = new HashSet<>();

    @Transient
    private Map<LabMetric.MetricType, Set<LabMetric>> ancestorMetricMap;
    @Transient
    private Map<LabMetric.MetricType, Set<LabMetric>> descendantMetricMap;

    @Transient
    private MaterialType latestMaterialType = null;

    protected LabVessel(String label) {
        createdOn = new Date();
        if (label == null || label.isEmpty() || label.equals("0")) {
            throw new RuntimeException("Invalid label " + label);
        }
        this.label = label;
    }

    protected LabVessel() {
    }

    public boolean isDNA() {
        return isMaterialType(MaterialType.DNA);
    }

    public boolean isMaterialType(MaterialType materialType) {
        boolean hasMaterialConvertedToMaterialType = hasMaterialConvertedTo(materialType);
        if (!hasMaterialConvertedToMaterialType) {
            for (String sampleMaterialType : getMaterialTypes()) {
                if (StringUtils.equalsIgnoreCase(materialType.getDisplayName(), sampleMaterialType)) {
                    return true;
                }
            }
        }
        return hasMaterialConvertedToMaterialType;
    }

    /**
     * Find the latest material type by first searching the event history then falling back on the sample's metadata.
     *
     * @return
     */
    public MaterialType getLatestMaterialType() {
        if (latestMaterialType == null) {
            latestMaterialType = getLatestMaterialTypeFromEventHistory();
            if (latestMaterialType == null || latestMaterialType == MaterialType.NONE) {
                Iterator<String> materialTypeIterator = getMaterialTypes().iterator();
                if (materialTypeIterator.hasNext()) {
                    String materialType = materialTypeIterator.next();
                    latestMaterialType = MaterialType.fromDisplayName(materialType);
                }
            }
        }
        if (latestMaterialType == MaterialType.NONE || latestMaterialType == null) {
            logger.error(String.format("No material type was found for vessel '%s'.", label));
        }
        return latestMaterialType;
    }

    public List<String> getMaterialTypes() {
        List<String> materialTypes = new ArrayList<>();
        for (SampleInstanceV2 si : getSampleInstancesV2()) {
            String materialType = si.getRootOrEarliestMercurySample().getSampleData().getMaterialType();
            if (StringUtils.isNotBlank(materialType)) {
                materialTypes.add(materialType);
            }
        }
        return materialTypes;
    }

    /**
     * Initializes SampleData for all vessels with data used when viewing Buckets.
     *
     * @param labVessels
     */
    public static void loadSampleDataForBuckets(Collection<LabVessel> labVessels) {
        SampleDataFetcher sampleDataFetcher = ServiceAccessUtility.getBean(SampleDataFetcher.class);
        Map<String, MercurySample> samplesBySampleKey = new HashMap<>();
        for (LabVessel labVessel : labVessels) {
            for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                MercurySample mercurySample = sampleInstanceV2.getRootOrEarliestMercurySample();
                samplesBySampleKey.put(mercurySample.getSampleKey(), mercurySample);
            }
        }

        Map<String, SampleData> sampleDataMap = sampleDataFetcher.fetchSampleData(samplesBySampleKey.keySet(),
                BSPSampleSearchColumn.BUCKET_PAGE_COLUMNS);
        for (Map.Entry<String, SampleData> sampleDataEntry : sampleDataMap.entrySet()) {
            if (sampleDataEntry.getValue() != null) {
                samplesBySampleKey.get(sampleDataEntry.getKey()).setSampleData(sampleDataEntry.getValue());
            }
        }
    }

    /**
     * Find the current MaterialType of this LabVesel.
     *
     * @param materialType materialType to test
     * @return true if the event history indicates the latest MaterialType matches input
     */
    private boolean hasMaterialConvertedTo(MaterialType materialType) {
        return materialType == getLatestMaterialTypeFromEventHistory();
    }

    /**
     * Traverse the event history of this LabVessel to find the current MaterialType.
     *
     * @return the current MaterialType
     */
    public MaterialType getLatestMaterialTypeFromEventHistory() {
        TransferTraverserCriteria.NearestMaterialTypeTraverserCriteria materialTypeTraverserCriteria =
                evaluateMaterialTypeTraverserCriteria();

        return materialTypeTraverserCriteria.getMaterialType();
    }

    /**
     * Traverser which scans the event history of this LabVessel to find the current MaterialType.
     */
    TransferTraverserCriteria.NearestMaterialTypeTraverserCriteria evaluateMaterialTypeTraverserCriteria() {
        TransferTraverserCriteria.NearestMaterialTypeTraverserCriteria materialTypeTraverserCriteria =
                new TransferTraverserCriteria.NearestMaterialTypeTraverserCriteria();
        evaluateCriteria(materialTypeTraverserCriteria, TransferTraverserCriteria.TraversalDirection.Ancestors);
        return materialTypeTraverserCriteria;
    }

    /**
     *  Check to see if this vessel is directly abandoned.
     *
     *  @return true if this vessel is abandoned
     *
     */
    @SuppressWarnings("unused") // used in JSP
    public boolean isVesselAbandoned() {
        if (getAbandonVessels().size() == 0) {
            return false;
        }
        return true;
    }

    /**
     *  Determine if the given vessel has multiple positions within it
     *
     */
    public boolean isMultiplePositions() {
        if(getGeometrySize() > 1) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     *  Returns the size of the vessel rows * columns.
     *
     */
    public int getGeometrySize() {
        return this.getVesselGeometry().getColumnCount() * this.getVesselGeometry().getRowCount();
    }

    /**
     * Label is typically the barcode of a lab vessel and must be unique in Mercury.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Warning this method also changes the equals and hashcode of this lab vessel entity.
     */
    public void setLabel(String label) {
        this.label = label;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<VesselToSectionTransfer> getVesselToSectionTransfersThisAsSource() {
        return vesselToSectionTransfersThisAsSource;
    }

    public Set<VesselToVesselTransfer> getVesselToVesselTransfersThisAsSource() {
        return vesselToVesselTransfersThisAsSource;
    }

    public Set<VesselToVesselTransfer> getVesselToVesselTransfersThisAsTarget() {
        return vesselToVesselTransfersThisAsTarget;
    }

    public void addMetric(LabMetric labMetric) {
        labMetrics.add(labMetric);
        labMetric.setLabVessel(this);
    }

    public void addAbandonedVessel(AbandonVessel abandonVessel) {
        abandonVessels.add(abandonVessel);
        abandonVessel.setAbandonedVessel(this);
    }

    public void removeAbandonedVessel(Set<AbandonVessel> abandonVessel) {
        this.abandonVessels.removeAll(abandonVessel);
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

    public LabMetric getMostRecentConcentration() {
        Set<LabMetric> concentrationMetrics = getConcentrationMetrics();
        if (concentrationMetrics == null || concentrationMetrics.isEmpty()) {
            return null;
        }
        List<LabMetric> metricList = new ArrayList<>(concentrationMetrics);
        metricList.sort(Collections.reverseOrder());
        return metricList.get(0);
    }

    @Nullable
    public LabMetricRun getMostRecentLabMetricRunForType(LabMetric.MetricType metricType) {
        if(labMetrics != null) {
            Set<LabMetric> metrics = new HashSet<>();
            for (LabMetric labMetric: labMetrics) {
                if(labMetric.getName()== metricType) {
                    metrics.add(labMetric);
                }
            }
            if (metrics.isEmpty()) {
                return null;
            }
            List<LabMetric> metricList = new ArrayList<>(metrics);
            metricList.sort(Collections.reverseOrder());
            return metricList.get(0).getLabMetricRun();
        }

        return null;
    }

    public StorageLocation getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(StorageLocation storageLocation) {
        this.storageLocation = storageLocation;
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

    public Set<LabVessel> getContainers() {
        if (containersCount != null && containersCount > 0) {
            return containers;
        }
        return Collections.emptySet();
    }

    public Set<VesselContainer<?>> getVesselContainers() {
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
        for( VesselToSectionTransfer vesselToSectionTransfer : vesselToSectionTransfersThisAsSource ) {
            transfersFrom.add(vesselToSectionTransfer.getLabEvent());
        }
        if (getContainerRole() == null) {

            for (VesselContainer<?> vesselContainer : getVesselContainers()) {
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
            for (VesselContainer<?> vesselContainer : getVesselContainers()) {
                transfersTo.addAll(vesselContainer.getTransfersTo());
            }
        } else {
            transfersTo.addAll(getContainerRole().getTransfersTo());
        }
        return transfersTo;
    }

    public Set<LabEvent> getTransfersToWithRearrays() {
        Set<LabEvent> transfersTo = new HashSet<>();
        for (VesselToVesselTransfer vesselToVesselTransfer : vesselToVesselTransfersThisAsTarget) {
            transfersTo.add(vesselToVesselTransfer.getLabEvent());
        }
        if (getContainerRole() == null) {
            for (VesselContainer<?> vesselContainer : getVesselContainers()) {
                transfersTo.addAll(vesselContainer.getTransfersToWithRearrays());
            }
        } else {
            transfersTo.addAll(getContainerRole().getTransfersToWithRearrays());
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
            for (VesselContainer<?> vesselContainer : getVesselContainers()) {
                transfersTo.addAll(vesselContainer.getTransfersToWithRearrays());
            }
        } else {
            transfersTo.addAll(getContainerRole().getTransfersToWithRearrays());
        }
        return transfersTo;
    }

    public abstract VesselGeometry getVesselGeometry();

    public Collection<JiraTicket> getJiraTickets() {
        if (ticketsCreatedCount != null && ticketsCreatedCount > 0) {
            return ticketsCreated;
        }
        return Collections.emptySet();
    }

    public void addJiraTicket(JiraTicket jiraTicket) {
        if (jiraTicket != null) {
            ticketsCreated.add(jiraTicket);
            if (ticketsCreatedCount == null) {
                ticketsCreatedCount = 0;
            }
            ticketsCreatedCount++;
        }
    }

    /**
     * Remove the association of the LabeVessel and the JiraTicket.
     */
    public void removeJiraTickets() {
        ticketsCreated.clear();
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
        if (inPlaceEventsCount != null && inPlaceEventsCount > 0) {
            return inPlaceLabEvents;
        }
        return Collections.emptySet();
    }

    public Set<LabEvent> getInPlaceEventsWithContainers() {
        Set<LabEvent> totalInPlaceEventsSet = Collections.unmodifiableSet(getInPlaceLabEvents());
        for (LabVessel vesselContainer : getContainers()) {
            totalInPlaceEventsSet = Sets.union(totalInPlaceEventsSet, vesselContainer.getInPlaceEventsWithContainers());
        }
        return totalInPlaceEventsSet;
    }

    /**
     * Get all events for vessel and containers sorted by date ascending
     */
    public List<LabEvent> getAllEventsSortedByDate() {
        Map<Date, LabEvent> sortedTreeMap = new TreeMap<>();
        for (LabEvent event : getEvents()) {
            sortedTreeMap.put(event.getEventDate(), event);
        }
        return new ArrayList<>(sortedTreeMap.values());
    }

    public void addInPlaceEvent(LabEvent labEvent) {
        inPlaceLabEvents.add(labEvent);
        labEvent.setInPlaceLabVessel(this);
        if (inPlaceEventsCount == null) {
            inPlaceEventsCount = 0;
        }
        inPlaceEventsCount++;
    }

    public abstract ContainerType getType();

    public static Collection<String> extractPdoKeyList(Collection<LabVessel> labVessels) {

        return extractPdoLabVesselMap(labVessels).keySet();
    }

    public static Map<String, Set<LabVessel>> extractPdoLabVesselMap(Collection<LabVessel> labVessels) {

        Map<String, Set<LabVessel>> vesselByPdoMap = new HashMap<>();

        for (LabVessel currVessel : labVessels) {
            Set<SampleInstanceV2> sampleInstances = currVessel.getSampleInstancesV2();

            for (SampleInstanceV2 sampleInstance : sampleInstances) {
                ProductOrderSample productOrderSample = sampleInstance.getSingleProductOrderSample();
                if (productOrderSample != null) {
                    String pdoKey = productOrderSample.getProductOrder().getJiraTicketKey();
                    if (!vesselByPdoMap.containsKey(pdoKey)) {
                        vesselByPdoMap.put(pdoKey, new HashSet<LabVessel>());
                    }
                    vesselByPdoMap.get(pdoKey).add(currVessel);
                }
            }
        }

        return vesselByPdoMap;
    }

    /**
     * This method gets a collection of the nearest lab metrics of the specified type. This only traverses ancestors.
     *
     * @param metricType The type of metric to search for during the traversal.
     *
     * @return A list of the closest metrics of the type specified, ordered by ascending date
     */
    public List<LabMetric> getNearestMetricsOfType(LabMetric.MetricType metricType) {
        return getNearestMetricsOfType(metricType, TransferTraverserCriteria.TraversalDirection.Ancestors);
    }

    /**
     * This method gets a collection of the nearest lab metrics of the specified type.
     *
     * @param metricType The type of metric to search for during the traversal.
     *
     * @return A list of the closest metrics of the type specified, ordered by ascending date
     */
    @Nullable
    public List<LabMetric> getNearestMetricsOfType(LabMetric.MetricType metricType,
            TransferTraverserCriteria.TraversalDirection traversalDirection) {
        if (getContainerRole() != null) {
            return getContainerRole().getNearestMetricOfType(metricType, traversalDirection);
        } else {
            TransferTraverserCriteria.NearestLabMetricOfTypeCriteria metricOfTypeCriteria =
                    new TransferTraverserCriteria.NearestLabMetricOfTypeCriteria(metricType);
            evaluateCriteria(metricOfTypeCriteria, traversalDirection);
            return metricOfTypeCriteria.getNearestMetrics();
        }
    }

    public void addNonReworkLabBatchStartingVessel(LabBatchStartingVessel labBatchStartingVessel) {
        labBatches.add(labBatchStartingVessel);
        if (labBatchesCount == null) {
            labBatchesCount = 0;
        }
        labBatchesCount++;
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

    public void setReceiptEvent(BspUser user, Date receivedDate, long disambiguator, String eventLocation) {
        LabEvent receiptEvent =
                new LabEvent(LabEventType.SAMPLE_RECEIPT, receivedDate, eventLocation,
                        disambiguator, user.getUserId(), LabEvent.UI_PROGRAM_NAME);
        addInPlaceEvent(receiptEvent);
    }

    public Triple<RackOfTubes, VesselPosition, String> findStorageContainer() {
        return null;
    }

    public Map<IlluminaFlowcell, Collection<VesselPosition>> getFlowcellLanesFrom() {
        Map<IlluminaFlowcell, Collection<VesselPosition>> illuminaFlowcells = new HashMap<>();
        LabVesselSearchDefinition.VesselsForEventTraverserCriteria eval
                = new LabVesselSearchDefinition.VesselsForEventTraverserCriteria(LabVesselSearchDefinition.FLOWCELL_LAB_EVENT_TYPES);
        evaluateCriteria(eval, TransferTraverserCriteria.TraversalDirection.Descendants);

        Map<LabVessel, Collection<VesselPosition>> labVesselCollectionMap = eval.getPositions().asMap();

        for(Map.Entry<LabVessel, Collection<VesselPosition>> labVesselAndPositions: labVesselCollectionMap.entrySet()) {
            IlluminaFlowcell flowcell =
                    OrmUtil.proxySafeCast(labVesselAndPositions.getKey(), IlluminaFlowcell.class);
            illuminaFlowcells.put(flowcell, labVesselAndPositions.getValue());
        }
        return illuminaFlowcells;
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

        private final LabVessel sourceLabVessel;
        private final LabVessel targetLabVessel;
        private final VesselContainer<?> sourceVesselContainer;
        private final VesselContainer<?> targetVesselContainer;
        private final VesselPosition sourcePosition;
        private final VesselPosition targetPosition;
        private final LabEvent labEvent;

        public VesselEvent(LabVessel sourceLabVessel, VesselContainer<?> sourceVesselContainer, VesselPosition sourcePosition,
                           LabEvent labEvent,
                           LabVessel targetLabVessel, VesselContainer<?> targetVesselContainer, VesselPosition targetPosition) {
            this.sourceLabVessel = sourceLabVessel;
            this.sourceVesselContainer = sourceVesselContainer;
            this.sourcePosition = sourcePosition;
            this.targetLabVessel = targetLabVessel;
            this.targetVesselContainer = targetVesselContainer;
            this.targetPosition = targetPosition;
            this.labEvent = labEvent;
        }

        public LabVessel getSourceLabVessel() {
            return sourceLabVessel;
        }

        public LabVessel getTargetLabVessel() {
            return targetLabVessel;
        }

        public LabEvent getLabEvent() {
            return labEvent;
        }

        public VesselPosition getSourcePosition() {
            return sourcePosition;
        }

        public VesselPosition getTargetPosition() {
            return targetPosition;
        }

        public VesselContainer<?> getSourceVesselContainer() {
            return sourceVesselContainer;
        }

        public VesselContainer<?> getTargetVesselContainer() {
            return targetVesselContainer;
        }
    }


    public int getSampleInstanceCount() {
        Set<SampleInstanceV2> sampleInstancesV2 = getSampleInstancesV2();
        if (sampleInstancesV2.size() == 1 && sampleInstancesV2.iterator().next().isReagentOnly()) {
            return 0;
        }
        return sampleInstancesV2.size();
    }

    /**
     * Get the immediate ancestor vessels (source) and a reference to this vessel (target) in the transfer path
     *
     * @return A list of ancestor vessel events
     */
    public List<VesselEvent> getAncestors() {
        List<VesselEvent> vesselEvents = new ArrayList<>();
        for (VesselToVesselTransfer vesselToVesselTransfer : vesselToVesselTransfersThisAsTarget) {
            VesselEvent vesselEvent = new VesselEvent(vesselToVesselTransfer.getSourceVessel(), null, null, vesselToVesselTransfer.getLabEvent(),
                    this, null, null);
            vesselEvents.add(vesselEvent);
        }
        for (LabVessel container : getContainers()) {
            vesselEvents.addAll(container.getContainerRole().getAncestors(this));
        }
        Collections.sort(vesselEvents, VesselEvent.COMPARE_VESSEL_EVENTS_BY_DATE);
        return vesselEvents;
    }

    /**
     * Get the immediate descendant vessels (target) and a reference to this vessel (source) in the transfer path
     *
     * @return A list of descendant vessel events
     */
    protected List<VesselEvent> getDescendants() {
        List<VesselEvent> vesselEvents = new ArrayList<>();
        for (VesselToVesselTransfer vesselToVesselTransfer : vesselToVesselTransfersThisAsSource) {
            VesselEvent vesselEvent = new VesselEvent(this, null, null, vesselToVesselTransfer.getLabEvent(), vesselToVesselTransfer.getTargetVessel(), null, null );
            vesselEvents.add(vesselEvent);
        }
        for (VesselToSectionTransfer vesselToSectionTransfer : vesselToSectionTransfersThisAsSource) {
            VesselEvent vesselEvent = new VesselEvent(this, null, null, vesselToSectionTransfer.getLabEvent(),
                    vesselToSectionTransfer.getTargetVesselContainer().getEmbedder(), null, null);
            vesselEvents.add(vesselEvent);
        }
        for (LabVessel container : getContainers()) {
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

    public BigDecimal getMass() {
        return mass;
    }

    public void setMass(BigDecimal mass) {
        this.mass = mass;
    }

    /**
     * Gets only the AbandonVessel entities directly attached to this lab vessel <br/>
     * Use TransferTraverserCriteria.AbandonedVesselCriteria For method of finding abandon state of ancestors and/or descendants
     * @see TransferTraverserCriteria.AbandonedLabVesselCriteria
     */
    public Set<AbandonVessel> getAbandonVessels() {
        return abandonVessels;
    }

    public Set<SampleInstanceEntity> getSampleInstanceEntities() {
        if (sampleInstanceEntitiesCount != null && sampleInstanceEntitiesCount > 0) {
            return sampleInstanceEntities;
        }
        return Collections.emptySet();
    }

    public void addSampleInstanceEntity(SampleInstanceEntity sampleInstanceEntity) {
        sampleInstanceEntities.add(sampleInstanceEntity);
        if (sampleInstanceEntitiesCount == null) {
            sampleInstanceEntitiesCount = 0;
        }
        sampleInstanceEntitiesCount++;
    }

    /**
     *  Get the AbandonVessel entry for a specific well <br/>
     *  Return null if well has not been abandoned.
     */
    @Nullable
    public AbandonVessel getAbandonPositionForWell( VesselPosition well ) {
        for (AbandonVessel abandonVessel : getAbandonVessels() ) {
            if( abandonVessel.getVesselPosition() == well ){
                return abandonVessel;
            }
        }
        return null;
    }

    public Set<BucketEntry> getBucketEntries() {
        if (bucketEntriesCount != null && bucketEntriesCount > 0) {
            return bucketEntries;
        }
        return Collections.emptySet();
    }

    public void removeBucketEntry(BucketEntry bucketEntry) {
        if (bucketEntries.remove(bucketEntry)) {
            bucketEntriesCount--;
        }
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
        bucketEntriesCount++;
        clearCaches();
    }

    public void addNonReworkLabBatch(LabBatch labBatch) {
        LabBatchStartingVessel startingVessel = new LabBatchStartingVessel(this, labBatch);
        labBatches.add(startingVessel);
        if (labBatchesCount == null) {
            labBatchesCount = 0;
        }
        labBatchesCount++;
    }

    public Set<LabBatch> getReworkLabBatches() {
        if (reworkLabBatchesCount != null && reworkLabBatchesCount > 0) {
            return reworkLabBatches;
        }
        return Collections.emptySet();
    }

    public void addReworkLabBatch(LabBatch reworkLabBatch) {
        reworkLabBatches.add(reworkLabBatch);
        if (reworkLabBatchesCount == null) {
            reworkLabBatchesCount = 0;
        }
        reworkLabBatchesCount++;
    }

    public void removeFromBatch(LabBatch labBatch) {
        for (LabBatchStartingVessel labBatchStartingVessel : getLabBatchStartingVessels()) {
            if (Objects.equals(labBatchStartingVessel.getLabBatch(), labBatch)) {
                labBatchStartingVessel.setLabVessel(null);
                labBatchStartingVessel.getLabBatch().getLabBatchStartingVessels().remove(labBatchStartingVessel);
                labBatches.remove(labBatchStartingVessel);
                labBatchesCount--;
                break;
            }
        }
        reworkLabBatches.remove(labBatch);
        reworkLabBatchesCount--;
    }

    public Set<LabBatch> getLabBatches() {
        Set<LabBatch> allLabBatches = new HashSet<>();
        for (LabBatchStartingVessel batchStartingVessel : getLabBatchStartingVessels()) {
            allLabBatches.add(batchStartingVessel.getLabBatch());
        }
        allLabBatches.addAll(getReworkLabBatches());
        return allLabBatches;
    }

    public List<LabBatch> getWorkflowLabBatches() {
        List<LabBatch> allLabBatches = new ArrayList<>();
        for (LabBatchStartingVessel batchStartingVessel : getLabBatchStartingVessels()) {
            if (batchStartingVessel.getLabBatch().getLabBatchType() == LabBatch.LabBatchType.WORKFLOW) {
                allLabBatches.add(batchStartingVessel.getLabBatch());
            }
        }
        allLabBatches.addAll(getReworkLabBatches());
        return allLabBatches;
    }

    public Set<LabBatchStartingVessel> getLabBatchStartingVessels() {
        if (labBatchesCount != null && labBatchesCount > 0) {
            return labBatches;
        }
        return Collections.emptySet();
    }

    // todo jmt cache this?
    public List<LabBatchStartingVessel> getLabBatchStartingVesselsByDate() {
        List<LabBatchStartingVessel> batchVesselsByDate = new ArrayList<>(getLabBatchStartingVessels());
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

    public void addDilutionReferences(LabBatchStartingVessel dilutionReferences) {
        this.dilutionReferences.add(dilutionReferences);
    }

    public Set<MercurySample> getMercurySamples() {
        if (mercurySamplesCount != null && mercurySamplesCount > 0) {
            return mercurySamples;
        }
        return Collections.emptySet();
    }

    public void clearSamples() {
        mercurySamples.clear();
        mercurySamplesCount = 0;
    }

    public void removeSample(MercurySample mercurySample) {
        if (mercurySamples.remove(mercurySample)) {
            mercurySamplesCount--;
        }
    }

    public void addSample(MercurySample mercurySample) {
        mercurySamples.add(mercurySample);
        if (mercurySamplesCount == null) {
            mercurySamplesCount = 0;
        }
        mercurySamplesCount++;
    }

    public void addAllSamples(Collection<MercurySample> mercurySamples) {
        this.mercurySamples.addAll(mercurySamples);
        if (mercurySamplesCount == null) {
            mercurySamplesCount = 0;
        }
        mercurySamplesCount += mercurySamples.size();
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
     * Initial call on vessel to begin to traverse nodes in the transfer graph and apply criteria.
     *
     * @param transferTraverserCriteria object that accumulates results of traversal
     * @param traversalDirection        ancestors or descendants
     */
    public void evaluateCriteria(TransferTraverserCriteria transferTraverserCriteria,
                                 TransferTraverserCriteria.TraversalDirection traversalDirection) {
        TransferTraverserCriteria.Context context = TransferTraverserCriteria.buildStartingContext(this, null, null, traversalDirection);
        TransferTraverserCriteria.TraversalControl traversalControl = transferTraverserCriteria.evaluateVesselPreOrder(
                context);
        if (traversalControl == TransferTraverserCriteria.TraversalControl.StopTraversing) {
            return;
        }
        evaluateCriteria(transferTraverserCriteria, traversalDirection, 1);
        transferTraverserCriteria.evaluateVesselPostOrder(context);
    }

    /**
     * Call during a traversal to visit nodes in the transfer graph and apply criteria.
     *
     * @param transferTraverserCriteria object that accumulates results of traversal
     * @param traversalDirection        ancestors or descendants
     */
    void evaluateCriteria(TransferTraverserCriteria transferTraverserCriteria,
                          TransferTraverserCriteria.TraversalDirection traversalDirection,
                          int hopCount) {
        TransferTraverserCriteria.Context context;
        List<VesselEvent> traversalNodes;

        // No need to traverse the same vessel multiple times
        if( transferTraverserCriteria.hasVesselBeenTraversed(this) ) {
            return;
        }

        if (traversalDirection == TransferTraverserCriteria.TraversalDirection.Ancestors) {
            traversalNodes = getAncestors();
        } else {
            traversalNodes = getDescendants();
        }

        boolean shouldStop = false;
        for( VesselEvent vesselEvent : traversalNodes ) {
            context = TransferTraverserCriteria.buildTraversalNodeContext(vesselEvent, hopCount, traversalDirection);
            TransferTraverserCriteria.TraversalControl traversalControl = transferTraverserCriteria.evaluateVesselPreOrder(context);
            if (traversalControl == TransferTraverserCriteria.TraversalControl.StopTraversing) {
                shouldStop = true;
            }
            // Finish up handling this hop before stopping
            if( !shouldStop ) {
                evaluateVesselEvent(transferTraverserCriteria,
                        traversalDirection,
                        hopCount,
                        vesselEvent);
            }
            transferTraverserCriteria.evaluateVesselPostOrder(context);
        }
    }

    private static void evaluateVesselEvent(TransferTraverserCriteria transferTraverserCriteria,
                                            TransferTraverserCriteria.TraversalDirection traversalDirection,
                                            int hopCount,
                                            VesselEvent vesselEvent) {
        LabVessel labVessel;
        if( traversalDirection == TransferTraverserCriteria.TraversalDirection.Ancestors ) {
            labVessel = vesselEvent.getSourceLabVessel();
            if (labVessel == null) {
                vesselEvent.getSourceVesselContainer().evaluateCriteria(vesselEvent.getSourcePosition(),
                        transferTraverserCriteria, traversalDirection,
                        hopCount);
            } else {
                labVessel.evaluateCriteria(transferTraverserCriteria, traversalDirection,
                        hopCount + 1);
            }
        } else {
            labVessel = vesselEvent.getTargetLabVessel();
            if (labVessel == null) {
                vesselEvent.getTargetVesselContainer().evaluateCriteria(vesselEvent.getTargetPosition(),
                        transferTraverserCriteria, traversalDirection,
                        hopCount);
            } else {
                labVessel.evaluateCriteria(transferTraverserCriteria, traversalDirection,
                        hopCount + 1);
            }
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

    public Collection<LabBatch> getNearestWorkflowLabBatches() {
        Set<LabBatch> workLabBatches = new HashSet<>();
        Set<SampleInstanceV2> sampleInstancesLocal;
        if (getContainerRole() != null) {
            sampleInstancesLocal = getContainerRole().getSampleInstancesV2();
        } else {
            sampleInstancesLocal = getSampleInstancesV2();
        }
        for (SampleInstanceV2 sampleInstance : sampleInstancesLocal) {
            if (sampleInstance.getSingleBatch() != null) {
                workLabBatches.add(sampleInstance.getSingleBatch());
            }
        }
        if (workLabBatches.isEmpty()) {
            // Vessel is used in more than a single lab batch, so use the lab batch with the latest creation date.
            for (SampleInstanceV2 sampleInstance : sampleInstancesLocal) {
                if (!sampleInstance.getAllWorkflowBatches().isEmpty()) {
                    workLabBatches.add(sampleInstance.getAllWorkflowBatches().get(
                            sampleInstance.getAllWorkflowBatches().size() - 1));
                }
            }
        }
        return workLabBatches;
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
        return findVesselsForLabEventType(type, useTargetVessels,
                EnumSet.allOf(TransferTraverserCriteria.TraversalDirection.class));
    }

    /**
     * This method iterates over all of the ancestor and descendant vessels, adding them to a map of vessel -> event
     * when the event matches the type given.
     *
     * @param type The type of event to filter the ancestors and descendants by.
     * @param useTargetVessels True if the vessels returned are event targets (vs. sources).
     * @param traversalDirections Direction(s) to traverse when searching for events
     *
     * @return A map of lab vessels keyed off the event they were present at filtered by type.
     */
    public Map<LabEvent, Set<LabVessel>> findVesselsForLabEventType(LabEventType type, boolean useTargetVessels,
                                                                    EnumSet<TransferTraverserCriteria.TraversalDirection> traversalDirections) {
        return findVesselsForLabEventTypes(Collections.singletonList(type), useTargetVessels, traversalDirections);
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
        return findVesselsForLabEventTypes(types, useTargetVessels,
                EnumSet.allOf(TransferTraverserCriteria.TraversalDirection.class));
    }

    /**
     * This method iterates over all of the ancestor and descendant vessels, adding them to a map of vessel -> event
     * when the event matches the type given.
     *
     * @param types A list of types of event to filter the ancestors and descendants by.
     * @param useTargetVessels True if the vessels returned are event targets (vs. sources).
     * @param traversalDirections Direction(s) to traverse when searching for events
     *
     * @return A map of lab vessels keyed off the event they were present at filterd by types.
     */
    public Map<LabEvent, Set<LabVessel>> findVesselsForLabEventTypes(List<LabEventType> types, boolean useTargetVessels,
                                                                     EnumSet<TransferTraverserCriteria.TraversalDirection> traversalDirections) {
        if (getContainerRole() != null) {
            return getContainerRole().getVesselsForLabEventTypes(types);
        }
        TransferTraverserCriteria.VesselForEventTypeCriteria vesselForEventTypeCriteria =
                new TransferTraverserCriteria.VesselForEventTypeCriteria(types, useTargetVessels);
        if (traversalDirections.contains(TransferTraverserCriteria.TraversalDirection.Ancestors)) {
            evaluateCriteria(vesselForEventTypeCriteria, TransferTraverserCriteria.TraversalDirection.Ancestors);
        }
        // Otherwise skips starting vessel because it's already been traversed
        vesselForEventTypeCriteria.resetAllTraversed();
        if (traversalDirections.contains(TransferTraverserCriteria.TraversalDirection.Descendants)) {
            evaluateCriteria(vesselForEventTypeCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
        }
        return vesselForEventTypeCriteria.getVesselsForLabEventType();
    }

    public Map<LabEvent, Set<LabVessel>> findVesselsForLabEventTypes(List<LabEventType> types,
            List<TransferTraverserCriteria.TraversalDirection> traversalDirections, boolean useTargetVessels) {
        if (getContainerRole() != null) {
            // todo jmt this is ancestors only
            return getContainerRole().getVesselsForLabEventTypes(types);
        }
        TransferTraverserCriteria.VesselForEventTypeCriteria vesselForEventTypeCriteria =
                new TransferTraverserCriteria.VesselForEventTypeCriteria(types, useTargetVessels);
        for (TransferTraverserCriteria.TraversalDirection traversalDirection : traversalDirections) {
            // Do not skip starting vessel when switching directions
            vesselForEventTypeCriteria.resetAllTraversed();
            evaluateCriteria(vesselForEventTypeCriteria, traversalDirection);
        }
        return vesselForEventTypeCriteria.getVesselsForLabEventType();
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

    /**
     * This method get index information for all samples in this vessel.
     *
     * @return a set of strings representing all indexes in this vessel.
     */
    public List<MolecularIndexReagent> getIndexes() {
        List<MolecularIndexReagent> indexes = new ArrayList<>();
        for (SampleInstanceV2 sample : getSampleInstancesV2()) {
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
        for (SampleInstanceV2 sampleInstance : getSampleInstancesV2()) {
            MercurySample curMercurySample = sampleInstance.getNearestMercurySample();
            if (curMercurySample != null) {
                if (curMercurySample.equals(sample)) {
                    indexes.addAll(getIndexesForSampleInstance(sampleInstance));
                }
            }
        }
        return indexes;
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

    // used in JSP
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

    /**
     * Returns the sample names of the most recent ancestors of samples in the vessel.
     */
    public Collection<String> getSampleNames() {
        return getSampleNames(true);
    }

    /**
     * Returns the sample names of all samples in the vessel, looking for either the most
     * recent ancestors or the root (or earliest) ancestors.
     */
    public Collection<String> getSampleNames(boolean useNearestSample) {
        Set<String> sampleNames = new HashSet<>();
        for (SampleInstanceV2 sampleInstance : getSampleInstancesV2()) {
            MercurySample sample = useNearestSample ?
                    sampleInstance.getNearestMercurySample() :
                    sampleInstance.getRootOrEarliestMercurySample();
            if (sample != null) {
                String sampleKey = StringUtils.trimToNull(sample.getSampleKey());
                if (sampleKey != null) {
                    sampleNames.add(sampleKey);
                }
            }
        }
        return sampleNames;
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
     * This method gets a map of all of the metrics from this vessel and all descendant vessels.
     *
     * @return Returns a map of lab metrics keyed by the metric type.
     */
    public Map<LabMetric.MetricType, Set<LabMetric>> getMetricsForVesselAndDescendants() {
        if (descendantMetricMap == null) {
            descendantMetricMap = buildMetricMap(getDescendantVessels());
        }
        return descendantMetricMap;
    }

    /**
     * This method gets a map of all of the metrics from this vessel and all ancestor vessels.
     *
     * @return Returns a map of lab metrics keyed by the metric display name.
     */
    public Map<LabMetric.MetricType, Set<LabMetric>> getMetricsForVesselAndAncestors() {
        if (ancestorMetricMap == null) {
            ancestorMetricMap = buildMetricMap(getAncestorVessels());
        }
        return ancestorMetricMap;
    }

    /**
     * Gets a map of all metrics for both ancestor and descendant vessels
     * @return Returns a map of lab metrics keyed by the metric display name.
     */
    public Map<LabMetric.MetricType, Set<LabMetric>> getMetricsForVesselAndRelatives(){
        Map<LabMetric.MetricType, Set<LabMetric>> allMetricMap = new HashMap<>();

        Map<LabMetric.MetricType, Set<LabMetric>> ancestorMetricMap = getMetricsForVesselAndAncestors();
        Map<LabMetric.MetricType, Set<LabMetric>> descendantMetricMap = getMetricsForVesselAndDescendants();

        allMetricMap.putAll(ancestorMetricMap);
        for ( Map.Entry<LabMetric.MetricType, Set<LabMetric>> mapEntry : descendantMetricMap.entrySet() ) {
            if( allMetricMap.containsKey( mapEntry.getKey() ) ) {
                allMetricMap.get(mapEntry.getKey()).addAll(mapEntry.getValue());
            } else {
                allMetricMap.put(mapEntry.getKey(), mapEntry.getValue());
            }
        }
        return allMetricMap;
    }

    private Map<LabMetric.MetricType, Set<LabMetric>> buildMetricMap(Collection<LabVessel> labVessels){
        Set<LabMetric> allMetrics = new HashSet<>();
        Map<LabMetric.MetricType, Set<LabMetric>> metricMap = new HashMap<>();
        allMetrics.addAll(getMetrics());
        for (LabVessel curVessel : labVessels) {
            allMetrics.addAll(curVessel.getMetrics());
        }
        Set<LabMetric> metricSet;
        for (LabMetric metric : allMetrics) {
            metricSet = metricMap.get(metric.getName());
            if (metricSet == null) {
                metricSet = new TreeSet<>();
                metricMap.put(metric.getName(), metricSet);
            }
            metricSet.add(metric);
        }
        return metricMap;
    }

    @Transient
    private Set<SampleInstanceV2> sampleInstances;

    public Set<SampleInstanceV2> getSampleInstancesV2() {
        if (sampleInstances == null) {
            sampleInstances = new TreeSet<>();
            if (getContainerRole() == null) {
                List<VesselEvent> ancestorEvents = getAncestors();
                if (ancestorEvents.isEmpty() || isRoot()) {
                    if(getSampleInstanceEntities().isEmpty()) {
                        sampleInstances.add(new SampleInstanceV2(this));
                    }
                    else {
                        for (SampleInstanceEntity sampleInstanceEntity : getSampleInstanceEntities()) {
                            sampleInstances.add(new SampleInstanceV2(this, sampleInstanceEntity));
                        }
                    }
                } else {
                    sampleInstances.addAll(VesselContainer.getAncestorSampleInstances(this, ancestorEvents));
                }
            } else {
                sampleInstances.addAll(getContainerRole().getSampleInstancesV2());
            }
        }
        return sampleInstances;
    }

    private boolean isRoot() {
        for (MercurySample mercurySample : getMercurySamples()) {
            Boolean isRoot = mercurySample.isRoot();
            if (isRoot != null && isRoot) {
                return true;
            }
        }

        return false;
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
        for (LabVessel container : containers) {
            container.clearCaches();
            container.getContainerRole().clearCaches();
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

        return LabEvent.isEventPresent(eventTraversalCriteria.getAllEvents(), labEventType);
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

    public void addState(State state) {
        states.add(state);
        state.addLabVessel(this);
    }
}
