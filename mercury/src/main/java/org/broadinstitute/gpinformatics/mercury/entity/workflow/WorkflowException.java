package org.broadinstitute.gpinformatics.mercury.entity.workflow;

/**
 *
 * The purpose of WorkflowException is to eliminate the need for throwing runtime exceptions in Workflow related
 * classes.  This will allow any catches of workflow exceptions to specifically target workflow exceptionsj.  Also
 * it will better track any issues that have come up.
 *
 * @author Scott Matthews
 *         Date: 4/17/13
 *         Time: 3:26 PM
 */
public class WorkflowException extends RuntimeException {

    public WorkflowException(String s) {
        super(s);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public WorkflowException(String s, Throwable throwable) {
        super(s, throwable);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public WorkflowException(Throwable throwable) {
        super(throwable);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
