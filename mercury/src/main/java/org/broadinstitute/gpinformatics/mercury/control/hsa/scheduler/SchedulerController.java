package org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler;

import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.ProcessTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;

import java.util.List;

public interface SchedulerController {
    List<PartitionInfo> listPartitions();
    List<QueueInfo> listQueue();
    String batchJob(String partition, ProcessTask processTask);
    boolean cancelJob(String jobId);
    Status fetchJobStatus(long pid);
    JobInfo fetchJobInfo(long jobId);
    boolean holdJobs(List<Long> jobIds);
    boolean releaseJobs(List<Long> jobIds);
}
