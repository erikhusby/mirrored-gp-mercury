package org.broadinstitute.gpinformatics.mercury.entity.workflow;

/**
 *
 * The purpose of WorkflowException is to eliminate the need for throwing runtime exceptions in Workflow related
 * classes.  This will allow any catches of workflow exceptions to specifically target workflow exceptions.  Also
 * it will better track any issues that have come up.
 *
 */
public class WorkflowException extends RuntimeException {

    public WorkflowException(String s) {
        super(s);
    }

    public WorkflowException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public WorkflowException(Throwable throwable) {
        super(throwable);
    }
}
