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
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
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
        if (workflows.isEmpty()) {
            return true;
        }
        for (ProductOrderAddOn orderAddon : productOrder.getAddOns()) {
            if (productHasWorkflow(orderAddon.getAddOn())) {
                return true;
            }
        }
        return false;
    }

    public Workflow findMatchingWorkflow(ProductOrder productOrder) {
        for (ProductOrderAddOn productOrderAddOn : productOrder.getAddOns()) {
            Workflow addonWorkflow = findMatchingWorkflow(productOrderAddOn.getAddOn());
            if (addonWorkflow != Workflow.NONE) {
                return addonWorkflow;
            }
        }
        return Workflow.NONE;
    }

    public Workflow findMatchingWorkflow(Product product) {
        Workflow productWorkflow = product.getWorkflow();
        if (productWorkflow != Workflow.NONE) {
            if (workflows.contains(productWorkflow)) {
                return productWorkflow;
            }
        }
        return Workflow.NONE;
    }

    public boolean productHasWorkflow(Product product) {
        if (workflows.isEmpty()){
            return true;
        }
        return findMatchingWorkflow(product)!=Workflow.NONE;
    }

    public boolean invoke(LabVessel labVessel, ProductOrder productOrder) {
        return productAddOnsHaveWorkflow(productOrder) && materialTypeMatches(labVessel);
    }


    public boolean invoke(LabVessel labVessel, Product product) {
        return productHasWorkflow(product) && materialTypeMatches(labVessel);
    }

}
