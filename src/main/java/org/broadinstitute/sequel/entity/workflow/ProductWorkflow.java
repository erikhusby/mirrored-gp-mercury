package org.broadinstitute.sequel.entity.workflow;

import java.util.ArrayList;
import java.util.List;

/**
 * The top-level workflow class
 */
public class ProductWorkflow {
    private String name;
    private List<WorkflowProcess> workflowProcesses = new ArrayList<WorkflowProcess>();
}
