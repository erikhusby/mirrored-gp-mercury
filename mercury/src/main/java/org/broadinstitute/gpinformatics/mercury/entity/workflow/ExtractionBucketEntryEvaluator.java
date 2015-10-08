/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2015 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

/**
 * An abstract BucketEntryEvaluator which tests if the labVessel is the materialType returned by getMaterialType().
 */
public abstract class ExtractionBucketEntryEvaluator implements BucketEntryEvaluator {
    @Override
    public boolean invoke(LabVessel labVessel) {
        return labVessel.isMaterialType(getMaterialType());
    }

    protected abstract LabVessel.MaterialType getMaterialType();
}
