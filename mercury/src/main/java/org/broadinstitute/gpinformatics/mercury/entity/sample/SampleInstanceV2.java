package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A transient class returned by LabVessel.getSampleInstances.  It accumulates information encountered
 * in a bottom-up traversal of LabEvents, from that LabVessel.
 */
public class SampleInstanceV2 implements Cloneable {

    private Set<MercurySample> rootMercurySamples = new HashSet<>();
    private List<Reagent> reagents = new ArrayList<>();
    private BucketEntry singleBucketEntry;
    private List<BucketEntry> allBucketEntries = new ArrayList<>();
    private LabBatch singleInferredBucketedBatch;
    private List<ProductOrderSample> allProductOrderSamples = new ArrayList<>();
    private List<LabBatchStartingVessel> allLabBatchStartingVessels = new ArrayList<>();

    /**
     * Constructs a sample instance from a LabVessel.
     */
    public SampleInstanceV2(LabVessel labVessel) {
        rootMercurySamples.addAll(labVessel.getMercurySamples());
        applyVesselChanges(labVessel);
    }

    /**
     * Returns the sample that has no incoming transfers.  This is typically the sample that BSP received from the
     * collaborator, but in the absence of extraction transfers, Mercury may not be able to follow the transfer chain
     * that far.
     */
    public String getMercuryRootSampleName() {
        return rootMercurySamples.iterator().next().getSampleKey();
    }

    /**
     * Returns all batches of the given type associated with ancestor vessels.  Pass type SAMPLES_IMPORT to get the
     * Aliquot.  Pass null to get all batches.
     */
    public List<LabBatchStartingVessel> getAllBatchVessels(LabBatch.LabBatchType labBatchType) {
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
     * transfer.
     */
    public BucketEntry getSingleBucketEntry() {
        return singleBucketEntry;
    }

    /**
     * Primarily for controls, which don't have BucketEntries.  The inference is based on LCSET calculated for each
     * transfer.
     * todo jmt not sure how useful this is.
     */
    public List<LabBatch> getAllInferredBucketedBatches() {
        return null;
    }

    /**
     * Primarily for controls, which don't have BucketEntries.  Returns the nearest bucketed batch, based on LCSET
     * calculated for each transfer.
     */
    public LabBatch getSingleInferredBucketedBatch() {
        return singleInferredBucketedBatch;
    }

    /**
     * Returns all Product Orders associated with samples in ancestor vessels.
     * todo jmt how should this be sorted?
     */
    public List<ProductOrderSample> getAllProductOrderSamples() {
        return allProductOrderSamples;
    }

    /**
     * Returns the nearest Product Order associated with samples in ancestor vessels.
     */
    public ProductOrderSample getSingleProductOrderSample() {
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
     * Makes a copy of an (ancestor) SampleInstance.
     */
    @Override
    public SampleInstanceV2 clone() throws CloneNotSupportedException {
        SampleInstanceV2 clone = (SampleInstanceV2) super.clone();
        clone.rootMercurySamples = new HashSet<>(rootMercurySamples);
        clone.reagents = new ArrayList<>(reagents);
        clone.allBucketEntries = new ArrayList<>(allBucketEntries);
        clone.allProductOrderSamples = new ArrayList<>(allProductOrderSamples);
        clone.allLabBatchStartingVessels = new ArrayList<>(allLabBatchStartingVessels);
        return clone;
    }

    /**
     * Applies to a clone any new information in a LabVessel.
     */
    public void applyVesselChanges(LabVessel labVessel) {
        reagents.addAll(labVessel.getReagentContents());
        allBucketEntries.addAll(labVessel.getBucketEntries());
        if (labVessel.getBucketEntries().size() == 1) {
            singleBucketEntry = labVessel.getBucketEntries().iterator().next();
        }
        allLabBatchStartingVessels.addAll(labVessel.getLabBatchStartingVesselsByDate());
        for (MercurySample mercurySample : labVessel.getMercurySamples()) {
            // todo jmt is this if statement correct?
            // If there is more than one transfer, we can't determine which transfer pertains to which ProductOrderSample.
//            if (labVessel.getTransfersFrom().size() < 2) {
                allProductOrderSamples.addAll(mercurySample.getProductOrderSamples());
//            }
        }
    }

    /**
     * Applies a LabEvent, specifically computed LCSets.
     */
    public void applyEvent(LabEvent labEvent) {
        if (singleBucketEntry == null) {
            Set<LabBatch> computedLcsets = labEvent.computeLcSets();
            if (computedLcsets.size() == 1) {
                singleInferredBucketedBatch = computedLcsets.iterator().next();
                if (allBucketEntries.size() > 1) {
                    for (BucketEntry bucketEntry : allBucketEntries) {
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
