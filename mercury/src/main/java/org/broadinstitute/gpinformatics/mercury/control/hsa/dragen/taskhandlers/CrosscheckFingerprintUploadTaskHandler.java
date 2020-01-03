package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;

import javax.enterprise.context.Dependent;

@Dependent
public class CrosscheckFingerprintUploadTaskHandler extends AbstractTaskHandler {

    @Override
    public void handleTask(Task task, SchedulerContext schedulerContext) {
        // TODO JW Implement
        task.setStatus(Status.COMPLETE);
    }
}
