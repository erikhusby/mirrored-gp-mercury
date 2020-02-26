package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers.AggregationMetricsTaskHandler;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers.AlignmentMetricsTaskHandler;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers.BclMetricsTaskHandler;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers.CrosscheckFingerprintUploadTaskHandler;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers.DemultiplexMetricsTaskHandler;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers.FingerprintTaskHandler;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers.PushIdatsToCloudHandler;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers.WaitForIdatTaskHandler;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers.WaitForInfiniumMetricsTaskHandler;
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
    private PushIdatsToCloudHandler pushIdatsToCloudHandler;

    @Inject
    private WaitForIdatTaskHandler waitForIdatTaskHandler;

    @Inject
    private WaitForInfiniumMetricsTaskHandler waitForInfiniumMetricsTaskHandler;

    public void fireEvent(Task task, SchedulerContext schedulerContext) throws InterruptedException {
        task.setStatus(Status.QUEUED);
        task.setQueuedTime(new Date());
        if (OrmUtil.proxySafeIsInstance(task, ProcessTask.class)) {
            if (OrmUtil.proxySafeIsInstance(task, PushIdatsToCloudTask.class)) {
                pushIdatsToCloudHandler.handleTask(task, schedulerContext);
            }
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
        }
    }

    private void handleStartProcess(Task task, SchedulerContext schedulerContext) {
        ProcessTask processTask = OrmUtil.proxySafeCast(task, ProcessTask.class);
        String pid = schedulerContext.getInstance().batchJob(processTask.getPartition(), processTask);
        processTask.setProcessId(Long.parseLong(pid));
    }

    public void checkTaskStatus(Task task, SchedulerContext schedulerContext) {
        Pair<Status, Date> statusDatepair = null;
        if (OrmUtil.proxySafeIsInstance(task, ProcessTask.class)) {
            ProcessTask processTask = OrmUtil.proxySafeCast(task, ProcessTask.class);
            if (processTask.getProcessId() == null) {
                statusDatepair = Pair.of(Status.QUEUED, null);
            } else {
                JobInfo jobInfo = schedulerContext.getInstance().fetchJobInfo(processTask.getProcessId());
                if (jobInfo == null) {
                    statusDatepair = Pair.of(Status.UNKNOWN, null);
                }
                Status status = jobInfo.getStatus();
                if (task.getStartTime() == null && jobInfo.getStart() != null) {
                    task.setStartTime(jobInfo.getStart());
                }
                statusDatepair = Pair.of(status, jobInfo.getEnd());
            }
        } else if (OrmUtil.proxySafeIsInstance(task, WaitForFileTask.class)) {
            WaitForFileTask waitForFileTask = OrmUtil.proxySafeCast(task, WaitForFileTask.class);
            File file = new File(waitForFileTask.getFilePath());

            Status status = file.exists() ? Status.COMPLETE : Status.RUNNING;
            Date endTime = file.exists() ? new Date(file.lastModified()) : null;
            statusDatepair = Pair.of(status, endTime);
        } if (OrmUtil.proxySafeIsInstance(task, WaitForInfiniumMetric.class)) {
            waitForInfiniumMetricsTaskHandler.handleTask(task, schedulerContext);
            if (task.isComplete()) {
                statusDatepair = Pair.of(Status.COMPLETE, new Date());
            } else {
                statusDatepair = Pair.of(task.getStatus(), null);
            }
        } else if (OrmUtil.proxySafeIsInstance(task, WaitForIdatTask.class)) {
            waitForIdatTaskHandler.handleTask(task, schedulerContext);
            if (task.isComplete()) {
                statusDatepair = Pair.of(Status.COMPLETE, new Date());
            } else {
                statusDatepair = Pair.of(Status.RUNNING, null);
            }
        } else if (OrmUtil.proxySafeIsInstance(task, WaitForReviewTask.class)) {
            return; // do nothing, wait for human review.
        } else if (OrmUtil.proxySafeIsInstance(task, WaitForCustomerFilesTask.class)) {
            return; // check portal?
        }

        task.setStatus(statusDatepair.getLeft());
        if (task.getStatus() == Status.COMPLETE) {
            task.setEndTime(statusDatepair.getRight());
        }
    }

    // For Testing
    public void setDemultiplexMetricsTaskHandler(
            DemultiplexMetricsTaskHandler demultiplexMetricsTaskHandler) {
        this.demultiplexMetricsTaskHandler = demultiplexMetricsTaskHandler;
    }

    public void setAlignmentMetricsTaskHandler(AlignmentMetricsTaskHandler alignmentMetricsTaskHandler) {
        this.alignmentMetricsTaskHandler = alignmentMetricsTaskHandler;
    }

    public void setPushIdatsToCloudHandler(PushIdatsToCloudHandler pushIdatsToCloudHandler) {
        this.pushIdatsToCloudHandler = pushIdatsToCloudHandler;
    }

    public void setWaitForIdatTaskHandler(WaitForIdatTaskHandler waitForIdatTaskHandler) {
        this.waitForIdatTaskHandler = waitForIdatTaskHandler;
    }

    public void setWaitForInfiniumMetricsTaskHandler(
            WaitForInfiniumMetricsTaskHandler waitForInfiniumMetricsTaskHandler) {
        this.waitForInfiniumMetricsTaskHandler = waitForInfiniumMetricsTaskHandler;
    }

    public PushIdatsToCloudHandler getPushIdatsToCloudHandler() {
        return pushIdatsToCloudHandler;
    }

    public WaitForIdatTaskHandler getWaitForIdatTaskHandler() {
        return waitForIdatTaskHandler;
    }

    public WaitForInfiniumMetricsTaskHandler getWaitForInfiniumMetricsTaskHandler() {
        return waitForInfiniumMetricsTaskHandler;
    }
}
