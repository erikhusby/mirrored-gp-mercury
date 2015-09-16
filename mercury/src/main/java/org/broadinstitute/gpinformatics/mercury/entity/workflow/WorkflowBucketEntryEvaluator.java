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
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A step in a process
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowBucketEntryEvaluator implements Serializable {
    Set<Workflow> workflows = new HashSet<>();
    Set<LabVessel.MaterialType> materialTypes = new HashSet<>();

    public WorkflowBucketEntryEvaluator() {
    }

    WorkflowBucketEntryEvaluator(Set<Workflow> workflows, Set<LabVessel.MaterialType> materialTypes) {
        this.workflows = workflows;
        this.materialTypes = materialTypes;
    }

    private boolean materialTypeMatches(LabVessel labVessel) {
        if (materialTypes.isEmpty()){
            return true;
        }
        for (LabVessel.MaterialType materialType : materialTypes) {
            if (labVessel.isMaterialType(materialType)) {
                return true;
            }
        }
        return false;
    }

    private boolean productAddOnsHaveWorkflow(ProductOrder productOrder) {
        if (workflows.isEmpty()){
            return true;
        }
        for (ProductOrderAddOn productOrderAddOn : productOrder.getAddOns()) {
            Workflow addOnWorkflow = productOrderAddOn.getAddOn().getWorkflow();
            if (addOnWorkflow != Workflow.NONE) {
                if (workflows.contains(addOnWorkflow)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean invoke(LabVessel labVessel, ProductOrder productOrder) {
        return productAddOnsHaveWorkflow(productOrder) && materialTypeMatches(labVessel);
    }
}
