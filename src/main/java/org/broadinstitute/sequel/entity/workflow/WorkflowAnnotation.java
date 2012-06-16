package org.broadinstitute.sequel.entity.workflow;

/**
 * Any kind of jBPMN extension in a workflow diagram is
 * consider a {@link WorkflowAnnotation}.
 */
public enum WorkflowAnnotation {

    /** Is the output of this step considered the zamboni single sample library? **/
    IS_SINGLE_SAMPLE_LIBRARY,
    /** Does this event result in durable, long term stable DNA? **/
    IS_DURABLE

}
