package org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler;

import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.ProcessTask;

import java.util.List;

public interface SchedulerController {
    List<PartitionInfo> listPartitions();
    List<QueueInfo> listQueue();
    String batchJob(String partition, ProcessTask processTask);
    boolean cancelJob(String jobId);
    boolean isJobComplete(String jobName, long pid);
    JobInfo fetchJobInfo(int jobId);
}
