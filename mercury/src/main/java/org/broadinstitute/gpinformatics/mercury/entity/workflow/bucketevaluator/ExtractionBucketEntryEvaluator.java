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
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;

import java.util.Collection;

/**
 * An abstract BucketEntryEvaluator which tests if the labVessel matches a materialType returned by getMaterialTypes()
 * and if a ProductOrder's add-on workflow supports extractions.
 */
public abstract class ExtractionBucketEntryEvaluator implements BucketEntryEvaluator {
    @Override
    public boolean invoke(LabVessel labVessel, ProductOrder productOrder) {
        return materialTypeMatches(labVessel) && productAddOnsHaveWorkflow(productOrder);
    }

    protected abstract Collection<Workflow> supportedWorkflows();

    protected abstract Collection<LabVessel.MaterialType> supportedMaterialTypes();

    private boolean materialTypeMatches(LabVessel labVessel) {
        for (LabVessel.MaterialType materialType : supportedMaterialTypes()) {
            if (labVessel.isMaterialType(materialType)) {
                return true;
            }
        }
        return false;
    }

    private boolean productAddOnsHaveWorkflow(ProductOrder productOrder) {
        for (ProductOrderAddOn productOrderAddOn : productOrder.getAddOns()) {
            Workflow addOnWorkflow = productOrderAddOn.getAddOn().getWorkflow();
            if (addOnWorkflow != Workflow.NONE) {
                if (supportedWorkflows().contains(addOnWorkflow)) {
                    return true;
                }
            }
        }
        return false;
    }

}
