package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers.AggregationMetricsTaskHandler;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers.AlignmentMetricsTaskHandler;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers.BclMetricsTaskHandler;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers.CrosscheckFingerprintUploadTaskHandler;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers.DeleteFolderTaskHandler;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers.DemultiplexMetricsTaskHandler;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers.FingerprintTaskHandler;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.JobInfo;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AggregationState;
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
    private BclMetricsTaskHandler bclMetricsTaskHandler;

    @Inject
    private AlignmentMetricsTaskHandler alignmentMetricsTaskHandler;

    @Inject
    private FingerprintTaskHandler fingerprintTaskHandler;

    @Inject
    private AggregationMetricsTaskHandler aggregationMetricsTaskHandler;

    @Inject
    private CrosscheckFingerprintUploadTaskHandler crosscheckFingerprintUploadTaskHandler;

    @Inject
    private DeleteFolderTaskHandler deleteFolderTaskHandler;

    public void fireEvent(Task task, SchedulerContext schedulerContext) throws InterruptedException {
        task.setStatus(Status.QUEUED);
        task.setQueuedTime(new Date());
        if (OrmUtil.proxySafeIsInstance(task, ProcessTask.class)) {
            handleStartProcess(task, schedulerContext);
            Thread.sleep(1000L); // Slurm Docs recommend a slight delay between job creation
        } else if (OrmUtil.proxySafeIsInstance(task, DemultiplexMetricsTask.class)) {
            demultiplexMetricsTaskHandler.handleTask(OrmUtil.proxySafeCast(task, DemultiplexMetricsTask.class) , schedulerContext);
        }  else if (OrmUtil.proxySafeIsInstance(task, BclDemultiplexMetricsTask.class)) {
            bclMetricsTaskHandler.handleTask(OrmUtil.proxySafeCast(task, BclDemultiplexMetricsTask.class), schedulerContext);
        } else if (OrmUtil.proxySafeIsInstance(task, AlignmentMetricsTask.class)) {
            if (OrmUtil.proxySafeIsInstance(task.getState(), AggregationState.class)) {
                aggregationMetricsTaskHandler.handleTask(OrmUtil.proxySafeCast(task, AlignmentMetricsTask.class), schedulerContext);
            } else {
                alignmentMetricsTaskHandler.handleTask(OrmUtil.proxySafeCast(task, AlignmentMetricsTask.class), schedulerContext);
            }
        } else if (OrmUtil.proxySafeIsInstance(task, FingerprintUploadTask.class)) {
            fingerprintTaskHandler.handleTask(OrmUtil.proxySafeCast(task, FingerprintUploadTask.class), schedulerContext);
        } else if (OrmUtil.proxySafeIsInstance(task, CrosscheckFingerprintUploadTask.class)) {
            crosscheckFingerprintUploadTaskHandler.handleTask(task, schedulerContext);
        } else if (OrmUtil.proxySafeIsInstance(task, WaitForReviewTask.class)) {
            // TODO JW handle better as its own state to avoid entering in the normal sense?
            task.setStatus(Status.RUNNING);
            task.getState().getFiniteStateMachine().setStatus(Status.TRIAGE);
        } else if (OrmUtil.proxySafeIsInstance(task, DeleteFolderTask.class)) {
            task.setStatus(Status.RUNNING);
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
                if (task.getStartTime() == null && jobInfo.getStart() != null) {
                    task.setStartTime(jobInfo.getStart());
                }
                Date end = jobInfo.getEnd();
                return Pair.of(status, end);
            }
        } else if (OrmUtil.proxySafeIsInstance(task, WaitForFileTask.class)) {
            WaitForFileTask waitForFileTask = OrmUtil.proxySafeCast(task, WaitForFileTask.class);
            File file = new File(waitForFileTask.getFilePath());

            Status status = file.exists() ? Status.COMPLETE : Status.RUNNING;
            Date endTime = file.exists() ? new Date(file.lastModified()) : null;
            return Pair.of(status, endTime);
        } else if (OrmUtil.proxySafeIsInstance(task, WaitForReviewTask.class)) {
            return Pair.of(Status.RUNNING, null);
        } else if (OrmUtil.proxySafeIsInstance(task, DeleteFolderTask.class)) {
            deleteFolderTaskHandler.handleTask(OrmUtil.proxySafeCast(task, DeleteFolderTask.class), schedulerContext);
            if (task.isComplete()) {
                return Pair.of(task.getStatus(), new Date());
            }
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
