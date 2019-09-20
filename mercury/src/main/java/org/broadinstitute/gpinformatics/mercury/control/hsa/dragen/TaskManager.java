package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers.AlignmentMetricsTaskHandler;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers.DemultiplexMetricsTaskHandler;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers.FingerprintTaskHandler;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.JobInfo;
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

    @Inject
    private FingerprintTaskHandler fingerprintTaskHandler;

    public void fireEvent(Task task, SchedulerContext schedulerContext) throws InterruptedException {
        task.setStatus(Status.RUNNING);
        task.setStartTime(new Date());
        if (OrmUtil.proxySafeIsInstance(task, ProcessTask.class)) {
            handleStartProcess(task, schedulerContext);
            Thread.sleep(1000L); // Slurm Docs recommend a slight delay between job creation
        } else if (OrmUtil.proxySafeIsInstance(task, DemultiplexMetricsTask.class)) {
            demultiplexMetricsTaskHandler.handleTask(task, schedulerContext);
        } else if (OrmUtil.proxySafeIsInstance(task, AlignmentMetricsTask.class)) {
            alignmentMetricsTaskHandler.handleTask(task, schedulerContext);
        } else if (OrmUtil.proxySafeIsInstance(task, FingerprintUploadTask.class)) {
            fingerprintTaskHandler.handleTask(task, schedulerContext);
        }
    }

    private void handleStartProcess(Task task, SchedulerContext schedulerContext) {
        ProcessTask processTask = OrmUtil.proxySafeCast(task, ProcessTask.class);
        String pid = schedulerContext.getInstance().batchJob(processTask.getPartition(), processTask);
        processTask.setProcessId(Long.parseLong(pid));
    }

    public Pair<Status, Date> checkTaskStatus(Task task, SchedulerContext schedulerContext) {
        if (OrmUtil.proxySafeIsInstance(task, ProcessTask.class)) {
            ProcessTask processTask = OrmUtil.proxySafeCast(task, ProcessTask.class);
            if (processTask.getProcessId() == null) {
                return Pair.of(Status.QUEUED, null);
            } else {
                JobInfo jobInfo = schedulerContext.getInstance().fetchJobInfo(processTask.getProcessId());
                if (jobInfo == null) {
                    return Pair.of(Status.UNKNOWN, null);
                }
                Status status = jobInfo.getStatus();
                Date end = jobInfo.getEnd();
                return Pair.of(status, end);
            }
        } else if (OrmUtil.proxySafeIsInstance(task, WaitForFileTask.class)) {
            WaitForFileTask waitForFileTask = OrmUtil.proxySafeCast(task, WaitForFileTask.class);
            File file = new File(waitForFileTask.getFilePath());

            Status status = file.exists() ? Status.COMPLETE : Status.RUNNING;
            Date endTime = file.exists() ? new Date(file.lastModified()) : null;
            return Pair.of(status, endTime);
        }

        return Pair.of(Status.UNKNOWN, null);
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
