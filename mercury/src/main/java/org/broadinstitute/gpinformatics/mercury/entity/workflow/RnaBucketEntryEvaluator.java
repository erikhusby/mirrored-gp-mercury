package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

/**
 * A BucketEntryEvaluator which allows bucketing if the labVessel has RNA in it.
 */
public class RnaBucketEntryEvaluator implements BucketEntryEvaluator{

    @Override
    public boolean invoke(LabVessel labVessel) {
        return labVessel.isMaterialType(LabVessel.MaterialType.RNA);
    }

}
