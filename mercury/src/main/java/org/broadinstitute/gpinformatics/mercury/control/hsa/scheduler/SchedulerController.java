package org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler;

import java.util.List;

public interface SchedulerController {
    List<PartitionInfo> listPartitions();
    List<QueueInfo> listQueue();
    String batchJob(String jobName, String partition, String script);
    boolean cancelJob(String jobId);
}
