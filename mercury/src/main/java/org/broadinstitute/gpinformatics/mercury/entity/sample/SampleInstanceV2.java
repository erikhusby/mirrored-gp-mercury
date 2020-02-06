package org.broadinstitute.gpinformatics.mercury.entity.sample;

import edu.mit.broad.prodinfo.thrift.lims.TZDevExperimentData;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.AnalysisType;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.DesignedReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UMIReagent;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.PositionLabBatches;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;

/**
 * A transient class returned by LabVessel.getSampleInstances.  It accumulates information encountered
 * in a bottom-up traversal of LabEvents, from that LabVessel.
 */
public class SampleInstanceV2 implements Comparable<SampleInstanceV2> {

    /**
     * Allows LabEvent.computeLabBatches to choose the nearest match if there are multiple.
     */
    public static class LabBatchDepth {
        private final int depth;
        private final LabBatch labBatch;

        public LabBatchDepth(int depth, @NotNull LabBatch labBatch) {
            this.depth = depth;
            this.labBatch = labBatch;
        }

        public int getDepth() {
            return depth;
        }

        public LabBatch getLabBatch() {
            return labBatch;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }

            LabBatchDepth labBatchDepth = (LabBatchDepth) obj;

            return labBatch.equals(labBatchDepth.labBatch);
        }

        @Override
        public int hashCode() {
            return labBatch.hashCode();
        }
    }

    private static final Log log = LogFactory.getLog(SampleInstanceV2.class);

    private Set<MercurySample> rootMercurySamples = new HashSet<>();
    private List<MercurySample> mercurySamples = new ArrayList<>();
    private List<Reagent> reagents = new ArrayList<>();
    private boolean isPooledTube;
    private String libraryName;
    private Integer readLength1;
    private Integer readLength2;
    private String aggregationParticle;
    private String aggregationDataType;
    private Boolean umisPresent;
    private String expectedInsertSize;
    private ReferenceSequence referenceSequence;
    private AnalysisType analysisType;
    private List<LabBatch> allWorkflowBatches = new ArrayList<>();
    private List<LabBatchDepth> allWorkflowBatchDepths = new ArrayList<>();
    private LabBatch singleWorkflowBatch;
    private List<BucketEntry> pendingBucketEntries = new ArrayList<>();
    private List<BucketEntry> allBucketEntries = new ArrayList<>();
    private BucketEntry singleBucketEntry;
    // todo jmt this doesn't include reworks
    private List<LabBatchStartingVessel> allLabBatchStartingVessels = new ArrayList<>();

    private List<ProductOrderSample> allProductOrderSamples = new ArrayList<>();
    private LabVessel initialLabVessel;
    private LabVessel currentLabVessel;
    private MolecularIndexingScheme molecularIndexingScheme;
    private LabVessel firstPcrVessel;
    private MaterialType materialType;
    private String baitName;
    private String catName;
    private int depth;
    private List<String> devConditions = new ArrayList<>();
    private TZDevExperimentData tzDevExperimentData = null;
    private Boolean impliedSampleName = null;
    private String externalRootSampleName = null;
    private Boolean pairedEndRead;
    private FlowcellDesignation.IndexType indexType;
    private Integer indexLength1;
    private Integer indexLength2;

    /**
     * For a reagent-only sample instance.
     */
    public SampleInstanceV2() {
    }

    /**
     * Constructs a sample instance from a LabVessel.
     */
    public SampleInstanceV2(LabVessel labVessel) {
        rootMercurySamples.addAll(labVessel.getMercurySamples());
        initiateSampleInstanceV2(labVessel);
        applyVesselChanges(labVessel, null);
    }

    /**
     * Constructs a sample instance from a LabVessel and an external library.
     */
    public SampleInstanceV2(LabVessel labVessel, SampleInstanceEntity sampleInstanceEntity) {
        rootMercurySamples.add(sampleInstanceEntity.getMercurySample());
        initiateSampleInstanceV2(labVessel);
        applyVesselChanges(labVessel, sampleInstanceEntity);
    }

    /**
     * Common setup for the above constructors.
     */
    private void initiateSampleInstanceV2(LabVessel labVessel)
    {
        initialLabVessel = labVessel;
        if (LabVessel.DIAGNOSTICS) {
            String message = "Created sample instance ";
            if (!labVessel.getMercurySamples().isEmpty()) {
                message += labVessel.getMercurySamples().iterator().next().getSampleKey();
            }
            message += " from " + labVessel.getLabel();
            log.info(message);
        }
        for (MercurySample rootMercurySample : rootMercurySamples) {
            for (Metadata metadata : rootMercurySample.getMetadata()) {
                if (metadata.getKey() == Metadata.Key.MATERIAL_TYPE) {
                    MaterialType metadataMaterialType = MaterialType.fromDisplayName(metadata.getValue());
                    if (metadataMaterialType != MaterialType.NONE) {
                        materialType = metadataMaterialType;
                        break;
                    }
                }
            }
        }
        depth = 0;
    }

    /**
     * Makes a copy of an (ancestor) SampleInstance.
     */
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public SampleInstanceV2(SampleInstanceV2 other) {
        // order of assignments is same as order of fields
        rootMercurySamples.addAll(other.rootMercurySamples);
        mercurySamples.addAll(other.mercurySamples);
        reagents.addAll(other.reagents);

        allWorkflowBatches.addAll(other.allWorkflowBatches);
        allWorkflowBatchDepths.addAll(other.allWorkflowBatchDepths);
        // Single workflow batch cannot be "inherited" if the ancestor has information that could change it.
        if (other.currentLabVessel == null || other.currentLabVessel.getWorkflowLabBatches().isEmpty()) {
            singleWorkflowBatch = other.singleWorkflowBatch;
        }
        allBucketEntries.addAll(other.allBucketEntries);
        pendingBucketEntries.addAll(other.pendingBucketEntries);
        singleBucketEntry = other.singleBucketEntry;

        allProductOrderSamples.addAll(other.allProductOrderSamples);
        allLabBatchStartingVessels.addAll(other.allLabBatchStartingVessels);
        molecularIndexingScheme = other.molecularIndexingScheme;
        initialLabVessel = other.initialLabVessel;
        firstPcrVessel = other.firstPcrVessel;
        materialType = other.materialType;
        depth = other.depth + 1;
        tzDevExperimentData = other.getTzDevExperimentData();
        devConditions = other.getDevConditions();
        isPooledTube = other.getIsPooledTube();
        libraryName = other.getLibraryName();
        readLength1 = other.getReadLength1();
        readLength2 = other.getReadLength2();
        baitName = other.getBaitName();
        catName = other.getCatName();
        aggregationParticle = other.getAggregationParticle();
        aggregationDataType = other.getAggregationDataType();
        analysisType = other.getAnalysisType();
        referenceSequence = other.getReferenceSequence();
        umisPresent = other.getUmisPresent();
        expectedInsertSize = other.getExpectedInsertSize();
        impliedSampleName = other.getImpliedSampleName();
        externalRootSampleName = other.getExternalRootSampleName();
        pairedEndRead = other.getPairedEndRead();
        indexType = other.getIndexType();
        indexLength1 = other.getIndexLength1();
        indexLength2 = other.getIndexLength2();
    }

    /**
     * Returns the sample that has no incoming transfers.  This is typically the sample that BSP received from the
     * collaborator, but in the absence of extraction transfers, Mercury may not be able to follow the transfer chain
     * that far.  May be null for a vessel that holds only reagents.  May be null for BSP samples that pre-date Mercury.
     */
    public String getMercuryRootSampleName() {
        if (rootMercurySamples.isEmpty()) {
            return null;
        }
        return rootMercurySamples.iterator().next().getSampleKey();
    }

    /**
     * Returns the earliest Mercury sample.  Tolerates unknown root sample.
     */
    public String getEarliestMercurySampleName() {
        return mercurySamples.isEmpty() ? null : mercurySamples.get(0).getSampleKey();
    }

    /**
     * Returns the name of the nearest Mercury sample in the transfer history.  Tolerates unknown root sample.
     */
    public String getNearestMercurySampleName() {
        return mercurySamples.isEmpty() ? null : mercurySamples.get(mercurySamples.size() - 1).getSampleKey();
    }

    /**
     * Returns the nearest Mercury sample in the transfer history.  Tolerates unknown root sample.
     */
    public MercurySample getNearestMercurySample() {
        return mercurySamples.isEmpty() ? null : mercurySamples.get(mercurySamples.size() - 1);
    }


    /**
     * Returns the root sample or if none, the earliest Mercury sample.
     */
    public MercurySample getRootOrEarliestMercurySample() {
        return CollectionUtils.isNotEmpty(rootMercurySamples) ?
                rootMercurySamples.iterator().next() : (mercurySamples.isEmpty() ? null : mercurySamples.get(0));
    }

    /**
     * Returns the name of the root sample or if none, the earliest Mercury sample.
     */
    public String getRootOrEarliestMercurySampleName() {
        return CollectionUtils.isNotEmpty(rootMercurySamples) ?
                rootMercurySamples.iterator().next().getSampleKey() :
                (mercurySamples.isEmpty() ? null : mercurySamples.get(0).getSampleKey());
    }

    /**
     * Returns all batches associated with ancestor vessels, sorted by increasing distance in the
     * transfer history.
     */
    public List<LabBatchStartingVessel> getAllBatchVessels() {
        return getAllBatchVessels(null);
    }

    /**
     * Returns all batches of the given type associated with ancestor vessels, sorted by increasing distance in the
     * transfer history.
     *
     * @param labBatchType Pass type SAMPLES_IMPORT to get the Aliquot.  Pass null to get all batches.
     */
    public List<LabBatchStartingVessel> getAllBatchVessels(LabBatch.LabBatchType labBatchType) {
        // Entities are added starting at the root, so reverse the list to sort by increasing distance.
        // todo jmt use a Deque?
        List<LabBatchStartingVessel> reverseBatchVessels = new ArrayList<>(allLabBatchStartingVessels);
        Collections.reverse(reverseBatchVessels);
        if (labBatchType == null) {
            return reverseBatchVessels;
        }
        // todo jmt cache by LabBatchType?
        List<LabBatchStartingVessel> labBatchStartingVessels = new ArrayList<>();
        for (LabBatchStartingVessel labBatchStartingVessel : reverseBatchVessels) {
            if (labBatchStartingVessel.getLabBatch().getLabBatchType() == labBatchType) {
                labBatchStartingVessels.add(labBatchStartingVessel);
            }
        }
        return labBatchStartingVessels;
    }

    /**
     * Returns the nearest batch.
     */
    public LabBatchStartingVessel getSingleBatchVessel() {
        return getSingleBatchVessel(null);
    }

    /**
     * Returns the vessel associated with this sample instance.
     */
    public LabVessel getInitialLabVessel() {
        return initialLabVessel;
    }

    /**
     * Returns the nearest batch of the given type.
     */
    public LabBatchStartingVessel getSingleBatchVessel(LabBatch.LabBatchType labBatchType) {
        for (int i = allLabBatchStartingVessels.size() - 1; i >= 0; i--) {
            LabBatchStartingVessel labBatchStartingVessel = allLabBatchStartingVessels.get(i);
            if (labBatchType == null) {
                return labBatchStartingVessel;
            } else {
                if (labBatchStartingVessel.getLabBatch().getLabBatchType() == labBatchType) {
                    return labBatchStartingVessel;
                }
            }
        }
        return null;
    }

    public List<LabBatch> getAllWorkflowBatches() {
        return allWorkflowBatches;
    }

    public List<LabBatchDepth> getAllWorkflowBatchDepths() {
        return allWorkflowBatchDepths;
    }

    /**
     * Returns all bucket entries associated with ancestor vessels.
     */
    public List<BucketEntry> getAllBucketEntries() {
        return allBucketEntries;
    }

    /**
     * Returns the most applicable BucketEntry associated with ancestor vessels, based on LCSET calculated for each
     * transfer.  May be null if a single BucketEntry can't be unambiguously determined.
     */
    @Nullable
    public BucketEntry getSingleBucketEntry() {
        return singleBucketEntry;
    }

    public List<BucketEntry> getPendingBucketEntries() {
        return pendingBucketEntries;
    }

    /**
     * Returns the batch from the single bucket entry, or the single inferred batch.
     */
    // todo jmt rename, fix comment
    public LabBatch getSingleBatch() {
        return singleWorkflowBatch;
    }

    /**
     * Returns all Product Orders associated with samples in ancestor vessels, sorted by increasing distance in the
     * transfer history.
     */
    public List<ProductOrderSample> getAllProductOrderSamples() {
        List<ProductOrderSample> reverseProductOrderSample = new ArrayList<>(allProductOrderSamples);
        Collections.reverse(reverseProductOrderSample);
        return reverseProductOrderSample;
    }

    /**
     * Returns the nearest Sample/Product Order associated with samples in ancestor vessels.
     */
    public ProductOrderSample getSingleProductOrderSample() {
        if (allProductOrderSamples.isEmpty()) {
            return null;
        }
        return allProductOrderSamples.get(allProductOrderSamples.size() - 1);
    }

    /**
     * Returns the nearest Sample/Product Order associated with the PDO of the single bucket entry in ancestor vessels. <br />
     * There are snapshots in time where a newer PDO sample may have been created but it hasn't been bucketed yet. <br />
     * This functionality gets the Sample/PDO associated with the single bucket.
     */
    public ProductOrderSample getProductOrderSampleForSingleBucket() {
        if (allProductOrderSamples.isEmpty() || singleBucketEntry == null) {
            return null;
        }
        ProductOrder bucketPDO = singleBucketEntry.getProductOrder();
        // Shouldn't happen, but if so, return latest
        if (bucketPDO == null) {
            return getSingleProductOrderSample();
        }

        for (int index = allProductOrderSamples.size() - 1; index >= 0; index--) {
            if (bucketPDO.equals(allProductOrderSamples.get(index).getProductOrder())) {
                return allProductOrderSamples.get(index);
            }
        }
        // No samples match bucket?  Let caller deal with null.
        return null;
    }

    /**
     * Returns baits, molecular indexes etc.
     */
    public List<Reagent> getReagents() {
        return reagents;
    }

    /**
     * Returns the workflow names associated with ancestor bucketed batches.
     */
    public String getWorkflowName() {
        if (singleWorkflowBatch != null) {
            return singleWorkflowBatch.getWorkflowName();
        }
        Set<String> workflowNames = new HashSet<>();
        for (LabBatch batch : allWorkflowBatches) {
            if (batch.getWorkflowName() != null) {
                workflowNames.add(batch.getWorkflowName());
            }
        }
        if (workflowNames.size() == 1) {
            return workflowNames.iterator().next();
        }
        return null;
    }

    @Nullable
    public MolecularIndexingScheme getMolecularIndexingScheme() {
        return molecularIndexingScheme;
    }

    @Nullable
    public Set<UMIReagent> getUmiReagents() {
        Set<UMIReagent> umiReagents = new HashSet<>();
        for (Reagent reagent: getReagents()) {
            if (OrmUtil.proxySafeIsInstance(reagent, UMIReagent.class)) {
                UMIReagent umiReagent =
                        OrmUtil.proxySafeCast(reagent, UMIReagent.class);
                umiReagents.add(umiReagent);
            }
        }
        return umiReagents;
    }

    public boolean isReagentOnly() {
        return mercurySamples.isEmpty() && !reagents.isEmpty();
    }

    /**
     * Adds a reagent encountered during transfer traversal.
     *
     * @param newReagent reagent to add
     */
    public void addReagent(Reagent newReagent) {
        if (LabVessel.DIAGNOSTICS) {
            log.info("Adding reagent " + newReagent.getName());
        }
        MolecularIndexingScheme molecularIndexingSchemeLocal = addReagent(newReagent, reagents);
        if (molecularIndexingSchemeLocal != null) {
            molecularIndexingScheme = molecularIndexingSchemeLocal;
        }
    }

    static MolecularIndexingScheme addReagent(Reagent newReagent, List<Reagent> reagents) {
        MolecularIndexingScheme returnMolecularIndexingScheme = null;
        // If we're adding a molecular index
        if (OrmUtil.proxySafeIsInstance(newReagent, MolecularIndexReagent.class)) {
            MolecularIndexReagent newMolecularIndexReagent =
                    OrmUtil.proxySafeCast(newReagent, MolecularIndexReagent.class);
            // Avoid adding the same index twice
            if (reagents.contains(newMolecularIndexReagent)) {
                return returnMolecularIndexingScheme;
            }
            boolean foundExistingIndex = false;
            boolean foundMergedScheme = false;
            // The new index has to be merged with other indexes encountered, if any.
            // E.g. If the field index is Illumina_P7-A, and the new index is Illumina_P5-B, we need a merged index
            // called Illumina_P5-B_P7-A.
            // Need to find a scheme that holds the field position / sequence combination(s) and the new
            // position / sequence combination(s).
            Iterator<Reagent> iterator = reagents.iterator();
            while (iterator.hasNext()) {
                Reagent fieldReagent = iterator.next();
                if (OrmUtil.proxySafeIsInstance(fieldReagent, MolecularIndexReagent.class)) {
                    foundExistingIndex = true;
                    MolecularIndexReagent fieldMolecularIndexReagent =
                            OrmUtil.proxySafeCast(fieldReagent, MolecularIndexReagent.class);
                    for (MolecularIndex molecularIndex : fieldMolecularIndexReagent.getMolecularIndexingScheme()
                            .getIndexes().values()) {
                        for (MolecularIndexingScheme molecularIndexingScheme : molecularIndex
                                .getMolecularIndexingSchemes()) {
                            Set<Map.Entry<MolecularIndexingScheme.IndexPosition, MolecularIndex>> entries =
                                    molecularIndexingScheme.getIndexes().entrySet();
                            if (entries.containsAll(
                                    fieldMolecularIndexReagent.getMolecularIndexingScheme().getIndexes().entrySet()) &&
                                    entries.containsAll(
                                            newMolecularIndexReagent.getMolecularIndexingScheme().getIndexes().entrySet())) {
                                foundMergedScheme = true;
                                iterator.remove();
                                reagents.add(new MolecularIndexReagent(molecularIndexingScheme));
                                returnMolecularIndexingScheme = molecularIndexingScheme;
                                break;
                            }
                        }
                    }
                    break;
                }
            }
            if (!foundExistingIndex) {
                reagents.add(newReagent);
                returnMolecularIndexingScheme = newMolecularIndexReagent.getMolecularIndexingScheme();
            } else if (!foundMergedScheme) {
                throw new RuntimeException("Failed to find merged molecular index scheme");
            }
        } else {
            reagents.add(newReagent);
        }
        return returnMolecularIndexingScheme;
    }

    /**
     * Applies to a clone any new information in a LabVessel and external library.
     */
    public final void applyVesselChanges(LabVessel labVessel, SampleInstanceEntity sampleInstanceEntity) {

        currentLabVessel = labVessel;

        // Merge in sample instance from the external upload.
        if (sampleInstanceEntity != null) {
            setIsPooledTube(true);
            MercurySample mercurySample = sampleInstanceEntity.getMercurySample();
            mergePooledTubeDevConditions(sampleInstanceEntity.getExperiment(), sampleInstanceEntity.getSubTasks());
            mergeMolecularIndex(sampleInstanceEntity.getMolecularIndexingScheme());
            mergeLibraryName(sampleInstanceEntity.getLibraryName());
            mergeReadLength(sampleInstanceEntity);
            aggregationParticle = sampleInstanceEntity.getAggregationParticle();
            aggregationDataType = sampleInstanceEntity.getAggregationDataType();
            analysisType = sampleInstanceEntity.getAnalysisType();
            referenceSequence = sampleInstanceEntity.getReferenceSequence();
            umisPresent = sampleInstanceEntity.getUmisPresent();
            expectedInsertSize = sampleInstanceEntity.getInsertSize();
            ReagentDesign reagentDesign = sampleInstanceEntity.getReagentDesign();
            catName = (reagentDesign != null && reagentDesign.getReagentType() == ReagentDesign.ReagentType.CAT) ?
                    reagentDesign.getDesignName() : null;
            baitName = (reagentDesign != null && reagentDesign.getReagentType() == ReagentDesign.ReagentType.BAIT) ?
                    reagentDesign.getDesignName() : sampleInstanceEntity.getBaitName();
            mercurySamples.add(mercurySample);
            impliedSampleName = sampleInstanceEntity.getImpliedSampleName();
            externalRootSampleName = sampleInstanceEntity.getMercurySample().getSampleData().getRootSample();
            pairedEndRead = sampleInstanceEntity.getPairedEndRead();
            indexType = sampleInstanceEntity.getIndexType();
            readLength1 = sampleInstanceEntity.getReadLength1();
            readLength2 = sampleInstanceEntity.getReadLength2();
            indexLength1 = sampleInstanceEntity.getIndexLength1();
            indexLength2 = sampleInstanceEntity.getIndexLength2();
        } else {
            mergeDevConditions(labVessel);
            mercurySamples.addAll(labVessel.getMercurySamples());
        }

        // order of assignments is same as order of fields
        for( Reagent reagent : labVessel.getReagentContents() ) {
            addReagent(reagent);
        }

        List<LabBatchStartingVessel> labBatchStartingVesselsByDate = labVessel.getLabBatchStartingVesselsByDate();
        allLabBatchStartingVessels.addAll(labBatchStartingVesselsByDate);
        // todo jmt sort by date?
        List<LabBatch> workflowLabBatches = labVessel.getWorkflowLabBatches();
        allWorkflowBatches.addAll(workflowLabBatches);
        for (LabBatch workflowLabBatch : workflowLabBatches) {
            allWorkflowBatchDepths.add(new LabBatchDepth(depth, workflowLabBatch));
        }
        if (allWorkflowBatches.size() == 1) {
            singleWorkflowBatch = allWorkflowBatches.get(0);
        }

        // todo jmt need a collection that includes pending bucket entries
        // filter out bucket entries without a lab batch
        Set<BucketEntry> bucketEntries = new HashSet<>();
        for (BucketEntry bucketEntry : labVessel.getBucketEntries()) {
            if (bucketEntry.getLabBatch() == null) {
                pendingBucketEntries.add(bucketEntry);
            } else {
                bucketEntries.add(bucketEntry);
            }
        }

        allBucketEntries.addAll(bucketEntries);
        if (bucketEntries.size() == 1) {
            singleBucketEntry = bucketEntries.iterator().next();
            if (LabVessel.DIAGNOSTICS) {
                log.info("Setting singleBucketEntry to " + singleBucketEntry.getLabBatch().getBatchName() +
                        " in " + labVessel.getLabel());
            }
        } else if (bucketEntries.size() > 1) { // todo jmt revisit
            singleBucketEntry = null;
        }

        for (MercurySample mercurySample : labVessel.getMercurySamples()) {
            allProductOrderSamples.addAll(mercurySample.getProductOrderSamples().stream().
                    sorted(Comparator.comparing(o -> o.getProductOrder().getProductOrderId() == null ? 0L : o.getProductOrder().getProductOrderId())).
                    collect(Collectors.toList()));
        }
    }

    /**
     * Applies a LabEvent, specifically computed LCSets.
     */
    public void applyEvent(LabVessel.VesselEvent vesselEvent, LabVessel labVessel) {
        LabEventType labEventType = vesselEvent.getLabEvent().getLabEventType();
        if (labEventType.getPipelineTransformation() == LabEventType.PipelineTransformation.PCR) {
            if (firstPcrVessel == null && labVessel != null) {
                firstPcrVessel = labVessel;
            }
        }
        MaterialType resultingMaterialType = labEventType.getResultingMaterialType();
        if (resultingMaterialType != null) {
            materialType = resultingMaterialType;
        }

        if (vesselEvent.getLabEvent().getManualOverrideLcSet() != null) {
            singleWorkflowBatch = vesselEvent.getLabEvent().getManualOverrideLcSet();
            setSingleBucketEntry(labEventType, singleWorkflowBatch);
        }
        // Multiple workflow batches need help.
        // Avoid overwriting a singleWorkflowBatch set by applyVesselChanges.
        else if (singleWorkflowBatch == null) {
            Set<LabBatch> computedLcsets = vesselEvent.getLabEvent().getComputedLcSets();
            // A single computed LCSET can help resolve ambiguity of multiple bucket entries.
            if (computedLcsets.size() != 1) {
                // Didn't get a single LCSET for the entire event, see if there's one for the vessel.
                PositionLabBatches posLabBatches = vesselEvent.getLabEvent().getMapPositionToLcSets().get(
                        vesselEvent.getTargetPosition());
                if (posLabBatches != null && !posLabBatches.getLabBatchSet().isEmpty()) {
                    computedLcsets = posLabBatches.getLabBatchSet();
                }
            }
            if (computedLcsets.size() == 1) {
                LabBatch workflowBatch = computedLcsets.iterator().next();
                singleWorkflowBatch = workflowBatch;
                setSingleBucketEntry(labEventType, workflowBatch);
            }
        }
    }

    private void setSingleBucketEntry(LabEventType labEventType, LabBatch workflowBatch) {
        for (BucketEntry bucketEntry : allBucketEntries) {
            // If there's a bucket entry that matches the computed LCSET, use it.
            if (bucketEntry.getLabBatch() != null &&
                    bucketEntry.getLabBatch().equals(workflowBatch)) {
                singleBucketEntry = bucketEntry;
                if (LabVessel.DIAGNOSTICS) {
                    log.info("Setting singleBucketEntry to " +
                            singleBucketEntry.getLabBatch().getBatchName() + " in " +
                            labEventType.getName());
                }
                break;
            }
        }
    }

    public Set<MercurySample> getRootMercurySamples() {
        return rootMercurySamples;
    }

    public LabVessel getFirstPcrVessel() {
        return firstPcrVessel;
    }

    @Nullable
    public MaterialType getMaterialType() {
        return materialType;
    }

    private void mergeReadLength(SampleInstanceEntity sampleInstanceEntity) {
        this.readLength1 = sampleInstanceEntity.getReadLength1();
        this.readLength2 = sampleInstanceEntity.getReadLength2();
    }

    public List<String> getDevConditions() { return devConditions; }

    public boolean getIsPooledTube() {
        return isPooledTube;
    }

    public void setIsPooledTube(boolean isPooledTube) {
        this.isPooledTube = isPooledTube;
    }

    private void mergeMolecularIndex(MolecularIndexingScheme molecularIndexingScheme)
    {
        this.molecularIndexingScheme = molecularIndexingScheme;
    }

    public Integer getReadLength1() {
        return readLength1;
    }

    public Integer getReadLength2() {
        return readLength2;
    }

    public String getLibraryName() {
        return libraryName;
    }

    public void mergeLibraryName(String libraryName) {
        this.libraryName = libraryName;
    }

    /**
     * Gets any ReagentDesign entities out of any DesignedReagent subclasses of Reagent on demand
     * to avoid the overhead of digesting Reagent into separate variables at SampleInstanceV2 creation
     * just to support a few use-cases
     */
    public Set<ReagentDesign> getReagentsDesigns() {
        Set<ReagentDesign> reagentDesigns = new HashSet<>();
        for( Reagent reagent : getReagents() ) {
            if (OrmUtil.proxySafeIsInstance(reagent, DesignedReagent.class)) {
                reagentDesigns.add( OrmUtil.proxySafeCast(reagent, DesignedReagent.class).getReagentDesign() );
            }
        }
        return reagentDesigns;
    }

    public Set<DesignedReagent> getDesignReagents() {
        Set<DesignedReagent> designReagents = new HashSet<>();
        for( Reagent reagent : getReagents() ) {
            if (OrmUtil.proxySafeIsInstance(reagent, DesignedReagent.class)) {
                designReagents.add( OrmUtil.proxySafeCast(reagent, DesignedReagent.class) );
            }
        }
        return designReagents;
    }

    private void mergePooledTubeDevConditions(String experimentName, List<String> subTasks)
    {
        // tzDevExperimentData must also be null to prevent resurrecting the experiment in DevExperimentDataBean.
        if (StringUtils.isNotBlank(experimentName)) {
            devConditions.addAll(subTasks);
            tzDevExperimentData = new TZDevExperimentData(experimentName, subTasks);
        }
    }

    private void mergeDevConditions(LabVessel labVessel)
    {
        // DEV Pooled tube upload is the only way to add experiment & conditions to a sample instance.
        if (!getIsPooledTube()) {
            for (JiraTicket ticket : labVessel.getJiraTickets()) {
                if (ticket != null) {
                    devConditions.add(ticket.getTicketId());
                }
            }
            if (devConditions.size() > 0) {
                //The experiment data will be populated from the parent Jira ticket.
                tzDevExperimentData = new TZDevExperimentData(null, devConditions);
            }
        }
    }


    public Date getLibraryCreationDate() {
        return getSingleBucketEntry().getLabVessel().getCreatedOn();
    }

    public TZDevExperimentData getTzDevExperimentData() {
        return this.tzDevExperimentData;
    }

    public String getAggregationParticle() {
        ProductOrderSample productOrderSample = getProductOrderSampleForSingleBucket();
        if (productOrderSample != null) {
            if (productOrderSample.getAggregationParticle() == null) {
                ProductOrder productOrder = productOrderSample.getProductOrder();
                Product.AggregationParticle defaultAggregationParticle = productOrder.getDefaultAggregationParticle();
                if (defaultAggregationParticle != null) {
                    return defaultAggregationParticle
                        .build(getNearestMercurySampleName(), productOrder.getJiraTicketKey());
                }
            } else {
                return productOrderSample.getAggregationParticle();
            }
        }

        return aggregationParticle;
    }

    public String getAggregationDataType() {
        return aggregationDataType;
    }

    public Boolean getUmisPresent() {
        return umisPresent;
    }

    public String getExpectedInsertSize() {
        return expectedInsertSize;
    }

    /** Returns the integer value of insert size. Returns the last value if an integer range such as 200-254. */
    public Integer getExpectedInsertSizeInteger() {
        if (StringUtils.isNotBlank(expectedInsertSize)) {
            String[] values = expectedInsertSize.split("[\\s-]");
            return Integer.parseInt(values[values.length - 1]);
        }
        return null;
    }

    /**
     * Returns a text description of the source
     * of the metadata.  Do not alter this string
     * without consulting the pipeline team.
     */
    public String getMetadataSourceForPipelineAPI() {
        Set<String> metadataSources = new HashSet<>();
        for (MercurySample mercurySample : mercurySamples) {
            metadataSources.add(mercurySample.getMetadataSourceForPipelineAPI());
        }
        if (metadataSources.isEmpty()) {
            throw new RuntimeException("Could not determine metadata source");
        }
        if (metadataSources.size() > 1) {
            throw new RuntimeException(String.format("Found %s metadata sources",metadataSources.size()));
        }
        return metadataSources.iterator().next();
    }

    public Boolean getImpliedSampleName() {
        return impliedSampleName;
    }

    public String getExternalRootSampleName() {
        return externalRootSampleName;
    }

    // todo jmt should these methods use nearest sample?
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        SampleInstanceV2 sampleInstanceV2 = (SampleInstanceV2) obj;

        if (molecularIndexingScheme != null ? !molecularIndexingScheme.equals(sampleInstanceV2.molecularIndexingScheme) :
                sampleInstanceV2.molecularIndexingScheme != null) {
            return false;
        }
        if (getEarliestMercurySampleName() != null ?
                !getEarliestMercurySampleName().equals(sampleInstanceV2.getEarliestMercurySampleName()) :
                sampleInstanceV2.getEarliestMercurySampleName() != null) {
            return false;
        }

        return true;
    }

    public ReferenceSequence getReferenceSequence() {
        return referenceSequence;
    }

    public AnalysisType getAnalysisType() {
        return analysisType;
    }

    public Boolean getPairedEndRead() {
        return pairedEndRead;
    }

    public FlowcellDesignation.IndexType getIndexType() {
        return indexType;
    }

    public Integer getIndexLength1() {
        return indexLength1;
    }

    public Integer getIndexLength2() {
        return indexLength2;
    }

    public String getBaitName() {
        return baitName;
    }

    public String getCatName() {
        return catName;
    }

    public String getSequencingLibraryName() {
        LabVessel pcrVessel = getFirstPcrVessel();
        if (pcrVessel != null) {
            String library = pcrVessel.getLabel();
            return String.format("%s_%s", library, getMolecularIndexingScheme().getName());
        } else { // Dev/SeqOnly samples start at pooling
            return String.format("%s_%s", getRootOrEarliestMercurySampleName(), getMolecularIndexingScheme().getName());
        }
    }

    public String getIndexingSchemeString() {
        MolecularIndexingScheme molecularIndexingScheme = getMolecularIndexingScheme();
        SortedMap<MolecularIndexingScheme.IndexPosition, MolecularIndex> indexes =
                molecularIndexingScheme.getIndexes();
        MolecularIndex p7 = indexes.get(MolecularIndexingScheme.IndexPosition.ILLUMINA_P7);
        String indexScheme = p7.getSequence();
        if (indexes.containsKey(MolecularIndexingScheme.IndexPosition.ILLUMINA_P5)) {
            MolecularIndex p5 = indexes.get(MolecularIndexingScheme.IndexPosition.ILLUMINA_P5);
            indexScheme = indexScheme + "_" + p5.getSequence();
        }

        return indexScheme;
    }

    @Override
    public int hashCode() {
        int result = getEarliestMercurySampleName() != null ? getEarliestMercurySampleName().hashCode() : 0;
        result = 31 * result + (molecularIndexingScheme != null ? molecularIndexingScheme.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(@NotNull SampleInstanceV2 o) {
        int compare = ObjectUtils.compare(getEarliestMercurySampleName(), o.getEarliestMercurySampleName());
        if (compare != 0) {
            return compare;
        }
        return ObjectUtils.compare(molecularIndexingScheme == null ? null : molecularIndexingScheme.getName(),
                o.getMolecularIndexingScheme() == null ? null : o.getMolecularIndexingScheme().getName());
    }
}
