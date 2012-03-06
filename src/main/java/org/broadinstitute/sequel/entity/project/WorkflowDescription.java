package org.broadinstitute.sequel.entity.project;

import org.broadinstitute.sequel.control.quote.PriceItem;

/**
 * A stub description of the end-to-end
 * workflow.  Things like "Hybrid Selection v8.0"
 * and "WGS 7.2"
 */
public class WorkflowDescription {

    private final String workflowName;

    private final String version;

    private PriceItem priceItem;
    
    public WorkflowDescription(String workflowName,
                               String version,
                               PriceItem priceItem) {
        if (workflowName == null) {
             throw new IllegalArgumentException("workflowName must be non-null in WorkflowDescription.WorkflowDescription");
        }
        this.workflowName = workflowName;
        this.version = version;
        this.priceItem = priceItem;  // nullable for dev work
    }

    public PriceItem getPriceItem() {
        return priceItem;
    }

    public String getWorkflowName() {
        return workflowName;
    }
    
    public String getWorkflowVersion() {
        return version;
    }

}
