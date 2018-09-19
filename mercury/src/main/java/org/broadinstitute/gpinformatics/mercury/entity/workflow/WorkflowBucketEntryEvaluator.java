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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
    private static final Log log = LogFactory.getLog(WorkflowBucketEntryEvaluator.class);
    Set<String> workflows = new HashSet<>();
    Set<MaterialType> materialTypes = new HashSet<>();

    public WorkflowBucketEntryEvaluator() {
    }

    WorkflowBucketEntryEvaluator(Set<String> workflows, Set<MaterialType> materialTypes) {
        this.workflows = workflows;
        this.materialTypes = materialTypes;
    }

    private boolean materialTypeMatches(MaterialType materialType) {
        if (materialTypes.isEmpty()){
            return true;
        }
        return materialTypes.contains(materialType);
    }

    private boolean productOrAddOnsHaveWorkflow(ProductOrder productOrder) {
        return workflows.isEmpty() || getMatchingWorkflow(productOrder) != null;
    }

    protected String getMatchingWorkflow(ProductOrder productOrder) {
        if (workflows.isEmpty()) {
            return productOrder.getProduct().getWorkflowName();
        }
        for (ProductOrderAddOn productOrderAddOn : productOrder.getAddOns()) {
            String matchingWorkflow = getMatchingWorkflow(productOrderAddOn.getAddOn().getWorkflowName());
            if (matchingWorkflow != null) {
                return matchingWorkflow;
            }
        }
        return getMatchingWorkflow(productOrder.getProduct().getWorkflowName());
    }

    private String getMatchingWorkflow(String workflow) {
        for (String testWorkflow : workflows) {
            if (testWorkflow.equals(workflow)) {
                return workflow;
            }
        }
        return null;
    }


    public boolean invoke(LabVessel labVessel, ProductOrder productOrder) {
        return productOrAddOnsHaveWorkflow(productOrder) && materialTypeMatches(labVessel.getLatestMaterialType());
    }

    protected String findMissingRequirements(ProductOrder productOrder, MaterialType materialType) {
        Set<MaterialType> missingMaterialTypes=materialTypes;
        missingMaterialTypes.remove(materialType);
        Set<String> missingWorkflows = workflows;
        missingWorkflows.removeAll(productOrder.getProductWorkflows());

        String missingRequirements = "";
        if (!missingMaterialTypes.isEmpty()) {
            missingRequirements =
                    String.format("Material Type: '%s' is not one of %s", materialType.getDisplayName(),
                            MaterialType.displayNamesOf(missingMaterialTypes));
        }
        if (!missingWorkflows.isEmpty()) {
            missingRequirements += String.format("Workflows '%s' are not one of %s ",
                    StringUtils.join(productOrder.getProductWorkflows(), ","), StringUtils.join(missingWorkflows, ","));
        }
        return missingRequirements;
    }
}
