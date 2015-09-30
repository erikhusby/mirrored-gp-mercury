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
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;

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
    Set<MaterialType> materialTypes = new HashSet<>();

    public WorkflowBucketEntryEvaluator() {
    }

    WorkflowBucketEntryEvaluator(Set<Workflow> workflows, Set<MaterialType> materialTypes) {
        this.workflows = workflows;
        this.materialTypes = materialTypes;
    }

    private boolean materialTypeMatches(LabVessel labVessel) {
        if (materialTypes.isEmpty()){
            return true;
        }
        return materialTypes.contains(labVessel.getLatestMaterialType());
    }

    private boolean productOrAddOnsHaveWorkflow(ProductOrder productOrder) {
        return workflows.isEmpty() || getMatchingWorkflow(productOrder) != Workflow.NONE;
    }

    public Workflow getMatchingWorkflow(ProductOrder productOrder) {
        if (workflows.isEmpty()) {
            return productOrder.getProduct().getWorkflow();
        }
        for (ProductOrderAddOn productOrderAddOn : productOrder.getAddOns()) {
            Workflow matchingWorkflow = getMatchingWorkflow(productOrderAddOn.getAddOn().getWorkflow());
            if (matchingWorkflow != Workflow.NONE) {
                return matchingWorkflow;
            }
        }
        return getMatchingWorkflow(productOrder.getProduct().getWorkflow());
    }

    private Workflow getMatchingWorkflow(Workflow workflow) {
        for (Workflow testWorkflow : workflows) {
            if (testWorkflow == workflow) {
                return workflow;
            }
        }
        return Workflow.NONE;
    }


    public boolean invoke(LabVessel labVessel, ProductOrder productOrder) {
        return productOrAddOnsHaveWorkflow(productOrder) && materialTypeMatches(labVessel);
    }

    public Set<MaterialType> getMaterialTypes() {
        return materialTypes;
    }

    public Set<Workflow> getWorkflows() {
        return workflows;
    }

    protected String findMissingRequirements(ProductOrder productOrder, MaterialType latestMaterialType) {
        Set<MaterialType> missingMaterialTypes=getMaterialTypes();
        missingMaterialTypes.remove(latestMaterialType);
        Set<Workflow> missingWorkflows=getWorkflows();
        missingWorkflows.removeAll(productOrder.getProduct().getProductWorkflows());

        String missingRequirements = "";
        if (!missingMaterialTypes.isEmpty()) {
            missingRequirements =
                    String.format("Material Type: '%s' is not one of %s", latestMaterialType, missingMaterialTypes);
        }
        if (!missingWorkflows.isEmpty()) {
            missingRequirements +=
                    String.format("Workflows '%s' are not one of %s ", productOrder.getProduct().getProductWorkflows(),
                            missingWorkflows);
        }
        return missingRequirements;
    }
}
