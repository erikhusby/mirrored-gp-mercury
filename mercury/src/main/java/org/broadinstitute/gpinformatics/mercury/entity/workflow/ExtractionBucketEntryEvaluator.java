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

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.Collection;

/**
 * An abstract BucketEntryEvaluator which tests if the labVessel is the materialType returned by getMaterialType().
 */
public abstract class ExtractionBucketEntryEvaluator implements BucketEntryEvaluator {
    @Override
    public boolean invoke(LabVessel labVessel) {
        return getMaterialTypes().contains(labVessel.getLatestMaterialTypeFromEventHistory()) &&
               productAndAddOnsHaveWorkflow(labVessel);
    }

    private boolean productAndAddOnsHaveWorkflow(LabVessel labVessel) {
        for (SampleInstanceV2 sampleInstance : labVessel.getSampleInstancesV2()) {
            ProductOrder productOrder = sampleInstance.getSingleProductOrderSample().getProductOrder();
            if (productOrder.getProduct().getWorkflow() == getWorkflow()) {
                for (ProductOrderAddOn productOrderAddOn : productOrder.getAddOns()) {
                    Workflow addOnWorkflow = productOrderAddOn.getAddOn().getWorkflow();
                    if (addOnWorkflow!=Workflow.NONE) {
                        if (addOnWorkflow == getWorkflow()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    protected abstract Workflow getWorkflow();
    protected abstract Collection<LabVessel.MaterialType> getMaterialTypes();
}
