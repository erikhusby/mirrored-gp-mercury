package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers.AlignmentMetricsTaskHandler;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers.DemultiplexMetricsTaskHandler;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.File;
import java.util.Date;

@Dependent
public class TaskManager {

    @Inject
    private DemultiplexMetricsTaskHandler demultiplexMetricsTaskHandler;

    @Inject
    private AlignmentMetricsTaskHandler alignmentMetricsTaskHandler;

    public void fireEvent(Task task, SchedulerContext schedulerContext) throws InterruptedException {
        task.setStatus(Status.RUNNING);
        task.setStartTime(new Date());
        if (OrmUtil.proxySafeIsInstance(task, ProcessTask.class)) {
            handleStartProcess(task, schedulerContext);
            // Slurm Docs recommend a slight delay between job creation
            Thread.sleep(1000L);
        } else if (OrmUtil.proxySafeIsInstance(task, DemultiplexMetricsTask.class)) {
            demultiplexMetricsTaskHandler.handleTask(task, schedulerContext);
        } else if (OrmUtil.proxySafeIsInstance(task, AlignmentMetricsTask.class)) {
            alignmentMetricsTaskHandler.handleTask(task, schedulerContext);
        }
    }

    private void handleStartProcess(Task task, SchedulerContext schedulerContext) {
        ProcessTask processTask = OrmUtil.proxySafeCast(task, ProcessTask.class);
        String pid = schedulerContext.getInstance().batchJob("dragen", processTask);
        processTask.setProcessId(Long.parseLong(pid));
    }

    public Status checkTaskStatus(Task task, SchedulerContext schedulerContext) {
        if (OrmUtil.proxySafeIsInstance(task, ProcessTask.class)) {
            ProcessTask processTask = OrmUtil.proxySafeCast(task, ProcessTask.class);
            return schedulerContext.getInstance().fetchJobStatus(processTask.getProcessId());
        } else if (OrmUtil.proxySafeIsInstance(task, WaitForFileTask.class)) {
            WaitForFileTask waitForFileTask = OrmUtil.proxySafeCast(task, WaitForFileTask.class);
            File file = new File(waitForFileTask.getFilePath());
            return file.exists() ? Status.COMPLETE : Status.RUNNING;
        }

        return Status.UNKNOWN;
    }

    // For Testing
    public void setDemultiplexMetricsTaskHandler(
            DemultiplexMetricsTaskHandler demultiplexMetricsTaskHandler) {
        this.demultiplexMetricsTaskHandler = demultiplexMetricsTaskHandler;
    }

    public void setAlignmentMetricsTaskHandler(AlignmentMetricsTaskHandler alignmentMetricsTaskHandler) {
        this.alignmentMetricsTaskHandler = alignmentMetricsTaskHandler;
    }
}
