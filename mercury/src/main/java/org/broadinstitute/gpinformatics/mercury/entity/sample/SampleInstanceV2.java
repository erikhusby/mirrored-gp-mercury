package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A transient class returned by LabVessel.getSampleInstances.  It accumulates information encountered
 * in a bottom-up traversal of LabEvents, from that LabVessel.
 */
public class SampleInstanceV2 {

    private static final Log log = LogFactory.getLog(SampleInstanceV2.class);

    private Set<MercurySample> rootMercurySamples = new HashSet<>();
    private List<MercurySample> mercurySamples = new ArrayList<>();
    private List<Reagent> reagents = new ArrayList<>();
    private BucketEntry singleBucketEntry;
    private List<BucketEntry> allBucketEntries = new ArrayList<>();
    private LabBatch singleInferredBucketedBatch;
    private List<ProductOrderSample> allProductOrderSamples = new ArrayList<>();
    private List<LabBatchStartingVessel> allLabBatchStartingVessels = new ArrayList<>();
    private LabVessel labVessel;
    private boolean examinedContainers;
    private MolecularIndexingScheme molecularIndexingScheme;

    /**
     * For a reagent-only sample instance.
     */
    public SampleInstanceV2() {
    }

    /**
     * Constructs a sample instance from a LabVessel.
     */
    public SampleInstanceV2(LabVessel labVessel) {
        this.labVessel = labVessel;
        rootMercurySamples.addAll(labVessel.getMercurySamples());
        mercurySamples.addAll(labVessel.getMercurySamples());
        if (LabVessel.DIAGNOSTICS) {
            String message = "Created sample instance ";
            if (!labVessel.getMercurySamples().isEmpty()) {
                message += labVessel.getMercurySamples().iterator().next().getSampleKey();
            }
            message += " from " + labVessel.getLabel();
            log.info(message);
        }
        applyVesselChanges(labVessel);
    }

    /**
     * Makes a copy of an (ancestor) SampleInstance.
     */
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public SampleInstanceV2(SampleInstanceV2 other) {
        rootMercurySamples.addAll(other.rootMercurySamples);
        mercurySamples.addAll(other.mercurySamples);
        reagents.addAll(other.reagents);
        singleBucketEntry = other.singleBucketEntry;
        allBucketEntries.addAll(other.allBucketEntries);
        singleInferredBucketedBatch = other.singleInferredBucketedBatch;
        allProductOrderSamples.addAll(other.allProductOrderSamples);
        allLabBatchStartingVessels.addAll(other.allLabBatchStartingVessels);
        molecularIndexingScheme = other.molecularIndexingScheme;
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
     * Primarily for controls, which don't have BucketEntries.  Returns the nearest bucketed batch, based on LCSET
     * calculated for each transfer.
     */
    public LabBatch getSingleInferredBucketedBatch() {
        if (singleInferredBucketedBatch == null && !examinedContainers) {
            if (labVessel != null) {
                // look at other tubes in same container(s).  If they're all of same LCSET, use it.
                Set<LabBatch> containedLabBatches = new HashSet<>();
                for (VesselContainer<?> vesselContainer : labVessel.getContainers()) {
                    for (LabVessel containedVessel : vesselContainer.getContainedVessels()) {
                        if (!containedVessel.equals(labVessel)) {
                            Set<SampleInstanceV2> sampleInstances = containedVessel.getSampleInstancesV2();
                            if (sampleInstances.size() == 1) {
                                BucketEntry containedSingleBucketEntry =
                                        sampleInstances.iterator().next().getSingleBucketEntry();
                                if (containedSingleBucketEntry != null) {
                                    containedLabBatches.add(containedSingleBucketEntry.getLabBatch());
                                }
                            }
                        }
                    }
                }
                if (containedLabBatches.size() == 1) {
                    singleInferredBucketedBatch = containedLabBatches.iterator().next();
                }
            }
            examinedContainers = true;
        }
        return singleInferredBucketedBatch;
    }

    /**
     * Returns the batch from the single bucket entry, or the single inferred batch.
     */
    public LabBatch getSingleBatch() {
        BucketEntry singleBucketEntryLocal = singleBucketEntry;
        if (singleBucketEntryLocal != null) {
            return singleBucketEntryLocal.getLabBatch();
        }
        return getSingleInferredBucketedBatch();
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
     * Returns the nearest Product Order associated with samples in ancestor vessels.
     */
    public ProductOrderSample getSingleProductOrderSample() {
        if (allProductOrderSamples.isEmpty()) {
            return null;
        }
        return allProductOrderSamples.get(allProductOrderSamples.size() - 1);
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
        if (singleBucketEntry != null && singleBucketEntry.getLabBatch() != null) {
            return singleBucketEntry.getLabBatch().getWorkflowName();
        }
        LabBatch singleInferredBucketedBatchLocal = getSingleInferredBucketedBatch();
        if (singleInferredBucketedBatchLocal != null && singleInferredBucketedBatchLocal.getWorkflowName() != null) {
            return singleInferredBucketedBatchLocal.getWorkflowName();
        }
/*
todo jmt not sure if this applies.
        String workflowName = null;
        for (LabBatch localLabBatch : allLabBatches) {
            if (localLabBatch.getWorkflowName() != null) {
                if (workflowName == null) {
                    workflowName = localLabBatch.getWorkflowName();
                } else {
                    if (!workflowName.equals(localLabBatch.getWorkflowName())) {
                        throw new RuntimeException("Conflicting workflows for sample " + sample.getSampleKey() + ": " +
                                workflowName + ", " + localLabBatch.getWorkflowName());
                    }
                }
            }
        }
*/
        return null; //workflowName;
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
            log.info("Adding reagent " + newReagent);
        }
        MolecularIndexingScheme molecularIndexingSchemeLocal = SampleInstance.addReagent(newReagent, reagents);
        if (molecularIndexingSchemeLocal != null) {
            molecularIndexingScheme = molecularIndexingSchemeLocal;
        }
    }

    /**
     * Applies to a clone any new information in a LabVessel.
     */
    public final void applyVesselChanges(LabVessel labVessel) {
        this.labVessel = labVessel;
        mercurySamples.addAll(labVessel.getMercurySamples());
        reagents.addAll(labVessel.getReagentContents());
        if (!labVessel.getBucketEntries().isEmpty()) {
            allBucketEntries.clear();
        }
        allBucketEntries.addAll(labVessel.getBucketEntries());
        if (labVessel.getBucketEntries().size() == 1) {
            singleBucketEntry = labVessel.getBucketEntries().iterator().next();
            if (LabVessel.DIAGNOSTICS) {
                log.info("Setting singleBucketEntry to " + singleBucketEntry.getLabBatch().getBatchName() +
                        " in " + labVessel.getLabel());
            }
        }
        allLabBatchStartingVessels.addAll(labVessel.getLabBatchStartingVesselsByDate());
        for (MercurySample mercurySample : labVessel.getMercurySamples()) {
            allProductOrderSamples.addAll(mercurySample.getProductOrderSamples());
        }
    }

    /**
     * Applies a LabEvent, specifically computed LCSets.
     */
    public void applyEvent(LabEvent labEvent) {
        Set<LabBatch> computedLcsets = labEvent.getComputedLcSets();
        // A single computed LCSET can help resolve ambiguity of multiple bucket entries.
        if (computedLcsets.size() == 1) {
            singleInferredBucketedBatch = computedLcsets.iterator().next();
            // Avoid overwriting a singleBucketEntry set by applyVesselChanges.
            if (singleBucketEntry == null) {
                // Multiple bucket entries need help.
                if (allBucketEntries.size() > 1) {
                    for (BucketEntry bucketEntry : allBucketEntries) {
                        // If there's a bucket entry that matches the computed LCSET, use it.
                        if (bucketEntry.getLabBatch() != null &&
                                bucketEntry.getLabBatch().equals(singleInferredBucketedBatch)) {
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
    }

    public Set<MercurySample> getRootMercurySamples() {
        return rootMercurySamples;
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
        else if (metadataSources.size() > 1) {
            throw new RuntimeException(String.format("Found %s metadata sources",metadataSources.size()));
        }
        return metadataSources.iterator().next();
    }

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

}
