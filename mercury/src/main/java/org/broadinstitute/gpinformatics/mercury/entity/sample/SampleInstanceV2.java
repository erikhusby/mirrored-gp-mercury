package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;

import java.util.List;

/**
 * A transient class returned by LabVessel.getSampleInstances.  It accumulates information encountered
 * in a bottom-up traversal from that LabVessel.
 */
public class SampleInstanceV2 {
    /**
     * Returns the sample that has no incoming transfers.  This is typically the sample that BSP received from the
     * collaborator, but in the absence of extraction transfers, Mercury may not be able to follow the transfer chain
     * that far.
     */
    public String getMercuryRootSampleName() {

    }

    /**
     * Returns all batches of the given type associated with ancestor vessels.  Pass type SAMPLES_IMPORT to get the
     * Aliquot.  Pass null to get all batches.
     */
    public List<LabBatchStartingVessel> getAllBatchVessels(LabBatch.LabBatchType) {

    }

    /**
     * Returns the nearest batch of the given type.
     */
    public LabBatchStartingVessel getSingleBatchVessel(LabBatch.LabBatchType) {

    }

    /**
     * Returns all bucket entries associated with ancestor vessels.
     */
    public List<BucketEntry> getAllBucketEntries() {

    }

    /**
     * Returns the most applicable BucketEntry associated with ancestor vessels, based on LCSET calculated for each
     * transfer.
     */
    public BucketEntry getSingleBucketEntry() {

    }

    /**
     * Primarily for controls, which don't have BucketEntries.  The inference is based on LCSET calculated for each
     * transfer.
     */
    public List<LabBatch> getAllInferredBucketedBatches() {

    }

    /**
     * Primarily for controls, which don't have BucketEntries.  Returns the nearest bucketed batch, based on LCSET
     * calculated for each transfer.
     */
    public LabBatch getSingleInferredBucketedBatch() {

    }

    /**
     * Returns all Product Orders associated with samples in ancestor vessels.
     */
    public List<ProductOrderSample> getAllProductOrderSamples() {

    }

    /**
     * Returns the nearest Product Order associated with samples in ancestor vessels.
     */
    public ProductOrderSample getSingleProductOrderSample() {

    }

    /**
     * Returns baits, molecular indexes etc.
     */
    public List<Reagent> getReagents() {

    }

    /**
     * Returns the workflow names associated with ancestor bucketed batches.
     */
    public List<String> getAllWorkflowNames() {

    }
}
