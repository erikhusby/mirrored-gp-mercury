package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
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

    private Set<MercurySample> rootMercurySamples = new HashSet<>();
    private List<Reagent> reagents = new ArrayList<>();
    private BucketEntry singleBucketEntry;
    private List<BucketEntry> allBucketEntries = new ArrayList<>();
    private LabBatch singleInferredBucketedBatch;
    private List<ProductOrderSample> allProductOrderSamples = new ArrayList<>();
    private List<LabBatchStartingVessel> allLabBatchStartingVessels = new ArrayList<>();

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
        applyVesselChanges(labVessel);
    }

    /**
     * Makes a copy of an (ancestor) SampleInstance.
     */
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public SampleInstanceV2(SampleInstanceV2 other) {
        rootMercurySamples.addAll(other.rootMercurySamples);
        reagents.addAll(other.reagents);
        singleBucketEntry = other.singleBucketEntry;
        allBucketEntries.addAll(other.allBucketEntries);
        singleInferredBucketedBatch = other.singleInferredBucketedBatch;
        allProductOrderSamples.addAll(other.allProductOrderSamples);
        allLabBatchStartingVessels.addAll(other.allLabBatchStartingVessels);
    }

    /**
     * Returns the sample that has no incoming transfers.  This is typically the sample that BSP received from the
     * collaborator, but in the absence of extraction transfers, Mercury may not be able to follow the transfer chain
     * that far.  May be null for a vessel that holds only reagents.
     */
    public String getMercuryRootSampleName() {
        if (rootMercurySamples.isEmpty()) {
            return null;
        }
        return rootMercurySamples.iterator().next().getSampleKey();
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
        return singleInferredBucketedBatch;
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
        if (singleInferredBucketedBatch != null && singleInferredBucketedBatch.getWorkflowName() != null) {
            return singleInferredBucketedBatch.getWorkflowName();
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
        return rootMercurySamples.isEmpty() && !reagents.isEmpty();
    }

    /**
     * Adds a reagent encountered during transfer traversal.
     *
     * @param newReagent reagent to add
     */
    public void addReagent(Reagent newReagent) {
        if (LabVessel.DIAGNOSTICS) {
            System.out.println("Adding reagent " + newReagent);
        }
        SampleInstance.addReagent(newReagent, reagents);
    }

    /**
     * Applies to a clone any new information in a LabVessel.
     */
    public final void applyVesselChanges(LabVessel labVessel) {
        reagents.addAll(labVessel.getReagentContents());
        allBucketEntries.addAll(labVessel.getBucketEntries());
        if (labVessel.getBucketEntries().size() == 1) {
            singleBucketEntry = labVessel.getBucketEntries().iterator().next();
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
                            break;
                        }
                    }
                }
            }
        }
    }

}
