package org.broadinstitute.sequel.entity.workflow;

/**
 * Where samples are placed for batching
 */
public class WorkflowBucket extends WorkflowStep{
    // rules for entry - material type, vol / conc ranges
    // auto-drain rules - time / date based
    public WorkflowBucket(String name) {
        super(name);
    }
}
