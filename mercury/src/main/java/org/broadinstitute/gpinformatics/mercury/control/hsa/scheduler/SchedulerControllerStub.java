package org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SchedulerControllerStub implements SchedulerController {

    private List<QueueInfo> queue = new ArrayList<>();

    @Override
    public List<PartitionInfo> listPartitions() {
        PartitionInfo broadPartition = new PartitionInfo();
        broadPartition.setAvailable("up");
        broadPartition.setName("broad");
        broadPartition.setNodeList("slurm-[0001-0002,0004-0005]");
        broadPartition.setState("idle");
        broadPartition.setNodes(4);
        broadPartition.setTimelimit("infinite");
        return Arrays.asList(broadPartition);
    }

    @Override
    public List<QueueInfo> listQueue() {
        QueueInfo queueInfo = new QueueInfo();

        return null;
    }

    @Override
    public String batchJob(String jobName, String partition, String script) {
        return null;
    }

    @Override
    public boolean cancelJob(String jobId) {
        Predicat
        queue.stream().remo
        return false;
    }
}
