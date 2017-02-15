package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A transient class returned by LabVessel.getSampleInstances.  It accumulates information encountered
 * in a bottom-up traversal of LabEvents, from that LabVessel.
 */
public class SampleInstanceV2 implements Comparable<SampleInstanceV2>{

    /**
     * Allows LabEvent.computeLcSets to choose the nearest match if there are multiple.
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

    private List<LabBatch> allWorkflowBatches = new ArrayList<>();
    private List<LabBatchDepth> allWorkflowBatchDepths = new ArrayList<>();
    private LabBatch singleWorkflowBatch;
    private List<BucketEntry> allBucketEntries = new ArrayList<>();
    private BucketEntry singleBucketEntry;
    // todo jmt this doesn't include reworks
    private List<LabBatchStartingVessel> allLabBatchStartingVessels = new ArrayList<>();

    private List<ProductOrderSample> allProductOrderSamples = new ArrayList<>();
    private LabVessel initialLabVessel;
    private LabVessel currentLabVessel;
    private MolecularIndexingScheme molecularIndexingScheme;
    private LabVessel firstPcrVessel;

    private int depth;

    /**
     * For a reagent-only sample instance.
     */
    public SampleInstanceV2() {
    }

    /**
     * Constructs a sample instance from a LabVessel.
     */
    public SampleInstanceV2(LabVessel labVessel) {
        initialLabVessel = labVessel;
        rootMercurySamples.addAll(labVessel.getMercurySamples());
        if (LabVessel.DIAGNOSTICS) {
            String message = "Created sample instance ";
            if (!labVessel.getMercurySamples().isEmpty()) {
                message += labVessel.getMercurySamples().iterator().next().getSampleKey();
            }
            message += " from " + labVessel.getLabel();
            log.info(message);
        }
        depth = 0;
        applyVesselChanges(labVessel);
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
        singleBucketEntry = other.singleBucketEntry;

        allProductOrderSamples.addAll(other.allProductOrderSamples);
        allLabBatchStartingVessels.addAll(other.allLabBatchStartingVessels);
        molecularIndexingScheme = other.molecularIndexingScheme;
        initialLabVessel = other.initialLabVessel;
        firstPcrVessel = other.firstPcrVessel;
        depth = other.depth + 1;
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

    /** Returns the name of the root sample or if none, the earliest Mercury sample. */
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
        for(int i = allLabBatchStartingVessels.size() - 1; i >= 0; i--) {
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
        if (allProductOrderSamples.isEmpty() || singleBucketEntry == null ) {
            return null;
        }
        ProductOrder bucketPDO = singleBucketEntry.getProductOrder();
        // Shouldn't happen, but if so, return latest
        if( bucketPDO == null ) {
            return getSingleProductOrderSample();
        }

        for( int index = allProductOrderSamples.size() - 1; index >= 0 ; index-- ) {
            if( bucketPDO.equals(allProductOrderSamples.get(index).getProductOrder())){
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
     * Applies to a clone any new information in a LabVessel.
     */
    public final void applyVesselChanges(LabVessel labVessel) {
        currentLabVessel = labVessel;
        // order of assignments is same as order of fields
        mercurySamples.addAll(labVessel.getMercurySamples());
        reagents.addAll(labVessel.getReagentContents());

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

        // filter out bucket entries without a lab batch
        Set<BucketEntry> bucketEntries = new HashSet<>();
        for (BucketEntry bucketEntry : labVessel.getBucketEntries()) {
            if (bucketEntry.getLabBatch() != null) {
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
            allProductOrderSamples.addAll(mercurySample.getProductOrderSamples());
        }
    }

    /**
     * Applies a LabEvent, specifically computed LCSets.
     */
    public void applyEvent(LabEvent labEvent, LabVessel labVessel) {
        if (labEvent.getLabEventType().getPipelineTransformation() == LabEventType.PipelineTransformation.PCR) {
            if (firstPcrVessel == null && labVessel != null) {
                firstPcrVessel = labVessel;
            }
        }
        // Multiple workflow batches need help.
        // Avoid overwriting a singleWorkflowBatch set by applyVesselChanges.
        if (singleWorkflowBatch == null) {
            Set<LabBatch> computedLcsets = labEvent.getComputedLcSets();
            // A single computed LCSET can help resolve ambiguity of multiple bucket entries.
            if (computedLcsets.size() != 1) {
                // Didn't get a single LCSET for the entire event, see if there's one for the vessel.
                LabVessel targetLabVessel = labEvent.getTargetLabVessels().iterator().next();
                VesselContainer<?> containerRole = targetLabVessel.getContainerRole();
                if (containerRole != null) {
                    Set<LabBatch> posLabBatches = labEvent.getMapPositionToLcSets().get(
                            containerRole.getPositionOfVessel(labVessel));
                    if (posLabBatches != null) {
                        computedLcsets = posLabBatches;
                    }
                }
            }
            if (computedLcsets.size() == 1) {
                LabBatch workflowBatch = computedLcsets.iterator().next();
                singleWorkflowBatch = workflowBatch;
                for (BucketEntry bucketEntry : allBucketEntries) {
                    // If there's a bucket entry that matches the computed LCSET, use it.
                    if (bucketEntry.getLabBatch() != null &&
                            bucketEntry.getLabBatch().equals(workflowBatch)) {
                        singleBucketEntry = bucketEntry;
                        if (LabVessel.DIAGNOSTICS) {
                            log.info("Setting singleBucketEntry to " +
                                    singleBucketEntry.getLabBatch().getBatchName() + " in " +
                                    labEvent.getLabEventType().getName());
                        }
                        break;
                    }
                }
            }
        }
    }

    public Set<MercurySample> getRootMercurySamples() {
        return rootMercurySamples;
    }

    public LabVessel getFirstPcrVessel() {
        return firstPcrVessel;
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

    // todo should this methods use nearest sample?
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
