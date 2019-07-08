package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerController;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import javax.enterprise.context.Dependent;
import java.io.File;

@Dependent
public class TaskManager {

    public void fireEvent(Task task, SchedulerContext schedulerContext) {
        task.setStatus(Status.RUNNING);
        if (OrmUtil.proxySafeIsInstance(task, ProcessTask.class)) {
            handleStartProcess(task, schedulerContext);
        }
    }

    private void handleStartProcess(Task task, SchedulerContext schedulerContext) {
        ProcessTask processTask = OrmUtil.proxySafeCast(task, ProcessTask.class);
        String pid = schedulerContext.getInstance().batchJob(null, processTask);
        processTask.setProcessId(Long.parseLong(pid));
    }

    public boolean isTaskComplete(Task task, SchedulerContext schedulerContext) {
        if (OrmUtil.proxySafeIsInstance(task, ProcessTask.class)) {
            return isProcessComplete(OrmUtil.proxySafeCast(task, ProcessTask.class), schedulerContext.getInstance());
        } else if (OrmUtil.proxySafeIsInstance(task, WaitForFileTask.class)) {
            WaitForFileTask waitForFileTask = OrmUtil.proxySafeCast(task, WaitForFileTask.class);
            File file = new File(waitForFileTask.getFilePath());
            return file.exists();
        }

        return false;
    }

    private boolean isProcessComplete(ProcessTask processTask, SchedulerController schedulerController) {
        return schedulerController.isJobComplete(processTask.getTaskName(), processTask.getProcessId());
    }
}
