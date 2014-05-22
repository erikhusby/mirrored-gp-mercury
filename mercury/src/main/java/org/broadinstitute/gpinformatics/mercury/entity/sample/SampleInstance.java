package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabBatchComposition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class is the result of a call to getSampleInstances on a {@link LabVessel}.  It holds: a sample (typically
 * imported from BSP); LabBatches to which the sample's plastic (including descendants of transfers) have been added;
 * reagents added to the sample's plastic (including descendants of transfers).
 * This class is currently transient, i.e. it is not persisted, but is created on demand by transfer traversal logic.
 */
public class SampleInstance {

    /**
     * Sample, from another system, typically BSP via Athena.
     */
    private final MercurySample sample;

    /**
     * Reagents added, e.g. molecular indexes, baits.
     */
    private final List<Reagent> reagents = new ArrayList<>();

    /**
     * The bucket entry for the single LCSET associated with the plastic on which getSampleInstances was called.
     * This is not set if traversal logic encounters multiple LCSETs, and can't pick a single one.
     */
    private BucketEntry bucketEntry;

    /**
     * Similar to bucketEntry, but controls aren't associated explicitly with LCSETs, so they don't have bucket
     * entries, so have to refer to the lab batch.
     */
    private LabBatch labBatch;

    // This gets set if the sample instance traverses a SAMPLE_IMPORT lab batch.
    private MercurySample bspExportSample;

    /**
     * All lab batches found during the traversal
     */
    private Collection<LabBatch> allLabBatches = new HashSet<>();

    private Set<LabBatch> workflowBatches;

    /**
     * The product order key for this sample instance. Can be set even if there are multiple bucket entries, as long as
     * all of the bucket entries are for the same product order.
     */
    private String productOrderKey;

    private boolean eventApplied = false;

    public boolean isEventApplied() {
        return eventApplied;
    }

    public void setEventApplied(boolean eventApplied) {
        this.eventApplied = eventApplied;
    }

    public SampleInstance(MercurySample sample) {
        this.sample = sample;
    }

    /**
     * Ultimately from whence this instance
     * was generated.  BSP aliquot?  BSP root?
     * Could also imagine "breaking" xfer history
     * in some way to separate things more thoroughly
     * (think of "just kiosk that" in the current
     * model, except that it would actually work)
     *
     * @return sample
     */
    public MercurySample getStartingSample() {
        return sample;
    }

    /**
     * Adds a reagent encountered during transfer traversal.
     *
     * @param newReagent reagent to add
     */
    public void addReagent(Reagent newReagent) {
        addReagent(newReagent, reagents);
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

    public List<Reagent> getReagents() {
        return reagents;
    }

    /**
     * This is set only when there is a single lab batch.
     *
     * @return lab batch
     */
    @Nullable
    public LabBatch getLabBatch() {
        if (bucketEntry != null) {
            return bucketEntry.getLabBatch();
        }
        return labBatch;
    }

    public void setLabBatch(LabBatch labBatch) {
        this.labBatch = labBatch;
    }

    public void setBucketEntry(@Nonnull BucketEntry bucketEntry) {
        this.bucketEntry = bucketEntry;
        productOrderKey = bucketEntry.getProductOrder().getBusinessKey();
    }

    public Collection<LabBatch> getAllLabBatches() {
        return allLabBatches;
    }

    public void addLabBatches(Collection<LabBatch> batches) {
        allLabBatches.addAll(batches);
    }

    public Collection<LabBatch> getAllWorkflowLabBatches() {
        if (workflowBatches == null) {
            workflowBatches = new HashSet<>();
            for (LabBatch batch : allLabBatches) {
                if (batch.getLabBatchType() == LabBatch.LabBatchType.WORKFLOW) {
                    workflowBatches.add(batch);
                }
            }
        }
        return workflowBatches;
    }

    /**
     * This method gets the batch compositions for this sample instance in the context of a lab vessel.
     *
     * @param vessel The lab vessel used as context to determine the batch compositions.
     *
     * @return An ordered list of batch compositions for this sample given the vessel as context. The most likely
     *         batch will be first in the list.
     */
    public List<LabBatchComposition> getLabBatchCompositionInVesselContext(LabVessel vessel) {
        List<LabBatchComposition> allLabBatchCompositions = vessel.getWorkflowLabBatchCompositions();
        List<LabBatchComposition> filteredBatchCompositions = new ArrayList<>();
        for (LabBatch labBatch : getAllWorkflowLabBatches()) {
            for (LabBatchComposition composition : allLabBatchCompositions) {
                if (composition.getLabBatch().equals(labBatch)) {
                    filteredBatchCompositions.add(composition);
                }
            }
        }
        Collections.sort(filteredBatchCompositions, LabBatchComposition.HIGHEST_COUNT_FIRST);

        return filteredBatchCompositions;
    }

    @Nullable
    public String getProductOrderKey() {
        return productOrderKey;
    }

    /**
     * Gets the name of the sample's workflow, based on LCSETs.
     *
     * @return workflow name
     */
    @Nullable
    public String getWorkflowName() {
        if (bucketEntry != null && bucketEntry.getLabBatch() != null) {
            return bucketEntry.getLabBatch().getWorkflowName();
        }
        if (labBatch != null && labBatch.getWorkflowName() != null) {
            return labBatch.getWorkflowName();
        }
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
        return workflowName;
    }

    public MercurySample getBspExportSample() {
        return bspExportSample;
    }

    public void setBspExportSample(MercurySample bspExportSample) {
        this.bspExportSample = bspExportSample;
    }

    public void setProductOrderKey(String productOrderKey) {
        this.productOrderKey = productOrderKey;
    }
}
