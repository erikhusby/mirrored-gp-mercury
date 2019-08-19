package org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler;

import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.Dragen;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DragenSimulator;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.ProcessTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.TaskResult;

import java.util.Arrays;
import java.util.List;

public class SchedulerControllerStub implements SchedulerController {

    private Dragen dragenSimulator = new DragenSimulator();

    @Override
    public List<PartitionInfo> listPartitions() {
        PartitionInfo broadPartition = new PartitionInfo();
        broadPartition.setAvailable("up");
        broadPartition.setName("broad");
        broadPartition.setNodeList("slurm-[0001-0002,0004-0005]");
        broadPartition.setState("idle");
        broadPartition.setNodes(4);
        broadPartition.setVersion("slurm 19.05.0");
        return Arrays.asList(broadPartition);
    }

    @Override
    public List<QueueInfo> listQueue() {
        QueueInfo queueInfo = new QueueInfo();
        queueInfo.setJobId("34541");
        queueInfo.setName("testname");
        queueInfo.setNodes(1);
        queueInfo.setPartition("broad");
        queueInfo.setState("PD");
        queueInfo.setUser("jowalsh");
        return null;
    }

    @Override
    public String batchJob(String partition, ProcessTask processTask) {
        TaskResult taskResult = dragenSimulator.fireProcess(processTask.getCommandLineArgument(), processTask);
        return String.valueOf(taskResult.getProcessId());
    }

    @Override
    public boolean cancelJob(String jobId) {
        return true;
    }

    @Override
    public Status fetchJobStatus(long pid) {
        return Status.COMPLETE;
    }

    @Override
    public JobInfo fetchJobInfo(long jobId) {
        return null;
    }
}
