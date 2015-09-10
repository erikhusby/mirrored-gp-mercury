package org.broadinstitute.gpinformatics.mercury.entity.workflow.bucketevaluator;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;

import java.util.Collection;
import java.util.EnumSet;

public class DnaAndRnaExtractionBucketEntryEvaluator extends ExtractionBucketEntryEvaluator {
    @Override
    protected Collection<Workflow> supportedWorkflows() {
        return EnumSet.of(Workflow.DNA_AND_RNA_EXTRACTION);
    }

    @Override
    protected Collection<LabVessel.MaterialType> supportedMaterialTypes() {
        return EnumSet.of(LabVessel.MaterialType.CELL_SUSPENSION, LabVessel.MaterialType.FRESH_FROZEN_TISSUE,
                LabVessel.MaterialType.FFPE, LabVessel.MaterialType.STOOL);
    }
}
