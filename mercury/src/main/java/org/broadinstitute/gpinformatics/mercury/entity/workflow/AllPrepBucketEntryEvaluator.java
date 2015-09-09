package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.EnumSet;
import java.util.Set;

public class AllPrepBucketEntryEvaluator extends ExtractionBucketEntryEvaluator {
    @Override
    protected Workflow getWorkflow() {
        return Workflow.DNA_AND_RNA_EXTRACTION;
    }

    @Override
    protected Set<LabVessel.MaterialType> getMaterialTypes() {
        return EnumSet.of(LabVessel.MaterialType.CELL_SUSPENSION, LabVessel.MaterialType.FRESH_FROZEN_TISSUE,
                LabVessel.MaterialType.FFPE, LabVessel.MaterialType.STOOL);
    }
}
