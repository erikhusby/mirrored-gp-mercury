package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Container for all workflow definition objects
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowConfig {

    /** List of processes, or lab teams */
    private List<WorkflowProcessDef> workflowProcessDefs = new ArrayList<WorkflowProcessDef>();

    /** List of product workflows, each composed of process definitions */
    private List<ProductWorkflowDef> productWorkflowDefs = new ArrayList<ProductWorkflowDef>();

    public List<WorkflowProcessDef> getWorkflowProcessDefs() {
        return workflowProcessDefs;
    }

    public List<ProductWorkflowDef> getProductWorkflowDefs() {
        return productWorkflowDefs;
    }

    void addWorkflowProcessDef(WorkflowProcessDef workflowProcessDef) {
        this.workflowProcessDefs.add(workflowProcessDef);
    }

    void addProductWorkflowDef(ProductWorkflowDef productWorkflowDef) {
        this.productWorkflowDefs.add(productWorkflowDef);
    }
}
