package org.broadinstitute.sequel.entity.workflow;

import java.util.ArrayList;
import java.util.List;

/**
 * Container for all workflow definition objects
 */
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
