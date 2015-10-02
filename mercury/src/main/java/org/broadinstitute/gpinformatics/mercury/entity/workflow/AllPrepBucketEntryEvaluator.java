package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

public class AllPrepBucketEntryEvaluator extends ExtractionBucketEntryEvaluator {
    @Override
    protected LabVessel.MaterialType getMaterialType() {
        return LabVessel.MaterialType.CELL_SUSPENSION;
    }
}
