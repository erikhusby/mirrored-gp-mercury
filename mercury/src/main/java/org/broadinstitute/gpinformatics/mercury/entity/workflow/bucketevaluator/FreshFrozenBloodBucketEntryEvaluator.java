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

package org.broadinstitute.gpinformatics.mercury.entity.workflow.bucketevaluator;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

public class FreshFrozenBloodBucketEntryEvaluator implements BucketEntryEvaluator {

    @Override
    public boolean invoke(LabVessel labVessel, ProductOrder productOrder) {
        return labVessel.isMaterialType(LabVessel.MaterialType.FRESH_FROZEN_BLOOD);
    }
}
