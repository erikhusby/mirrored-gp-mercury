package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A transient class returned by LabVessel.getSampleInstances.  It accumulates information encountered
 * in a bottom-up traversal from that LabVessel.
 */
public class SampleInstanceV2 implements Cloneable {

    private Set<MercurySample> rootMercurySamples = new HashSet<>();

    /**
     * Reagents added, e.g. molecular indexes, baits.
     */
    private final List<Reagent> reagents = new ArrayList<>();

    public SampleInstanceV2(LabVessel labVessel) {
        rootMercurySamples.addAll(labVessel.getMercurySamples());
        reagents.addAll(labVessel.getReagentContents());
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
        return null;
    }

    /**
     * Returns the nearest batch of the given type.
     */
    public LabBatchStartingVessel getSingleBatchVessel(LabBatch.LabBatchType labBatchType) {
        return null;
    }

    /**
     * Returns all bucket entries associated with ancestor vessels.
     */
    public List<BucketEntry> getAllBucketEntries() {
        return null;
    }

    /**
     * Returns the most applicable BucketEntry associated with ancestor vessels, based on LCSET calculated for each
     * transfer.
     */
    public BucketEntry getSingleBucketEntry() {
        return null;
    }

    /**
     * Primarily for controls, which don't have BucketEntries.  The inference is based on LCSET calculated for each
     * transfer.
     */
    public List<LabBatch> getAllInferredBucketedBatches() {
        return null;
    }

    /**
     * Primarily for controls, which don't have BucketEntries.  Returns the nearest bucketed batch, based on LCSET
     * calculated for each transfer.
     */
    public LabBatch getSingleInferredBucketedBatch() {
        return null;
    }

    /**
     * Returns all Product Orders associated with samples in ancestor vessels.
     */
    public List<ProductOrderSample> getAllProductOrderSamples() {
        return null;
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
    public List<String> getAllWorkflowNames() {
        return null;
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
        SampleInstance.addReagent(newReagent, reagents);
    }

    @Override
    public SampleInstanceV2 clone() throws CloneNotSupportedException {
        return (SampleInstanceV2) super.clone();
    }

    public void applyChanges(LabVessel labVessel) {

    }
}
