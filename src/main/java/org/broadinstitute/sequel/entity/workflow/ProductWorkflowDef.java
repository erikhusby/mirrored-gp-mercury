package org.broadinstitute.sequel.entity.workflow;

import java.util.ArrayList;
import java.util.List;

/**
 * The workflow definition for a product, composed of processes
 */
public class ProductWorkflowDef {
    private String name;
    private List<WorkflowProcessDef> workflowProcessDefs = new ArrayList<WorkflowProcessDef>();
    private List<String> entryPointsUsed;
}
