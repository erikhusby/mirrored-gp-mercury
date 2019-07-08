package org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.zeroturnaround.exec.ProcessOutput;
import org.zeroturnaround.exec.ProcessResult;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

public class SlurmControllerTest {

    private ShellUtils shellUtils;
    private SlurmController slurmController;

    @BeforeMethod
    public void setUp() {
        shellUtils = mock(ShellUtils.class);
        slurmController = new SlurmController();
        slurmController.setShellUtils(shellUtils);
    }

    @Test
    public void testListPartitions() throws Exception {
        List<String> cmd = Arrays.asList("sinfo", "-o", "\"%all\"");
        String output = "AVAIL|ACTIVE_FEATURES|CPUS|TMP_DISK|FREE_MEM|AVAIL_FEATURES|GROUPS|OVERSUBSCRIBE|TIMELIMIT|MEMORY|HOSTNAMES|NODE_ADDR|PRIO_TIER|ROOT|JOB_SIZE|STATE|USER|VERSION|WEIGHT|S:C:T|NODES(A/I) |MAX_CPUS_PER_NODE |CPUS(A/I/O/T) |NODES |REASON |NODES(A/I/O/T) |GRES |TIMESTAMP |PRIO_JOB_FACTOR |DEFAULTTIME |PREEMPT_MODE |NODELIST |CPU_LOAD |PARTITION |PARTITION |ALLOCNODES |STATE |USER |CLUSTER |SOCKETS |CORES |THREADS \n"
                        + "up|(null)|16|0|116181|(null)|all|NO|infinite|128714|slurm-0001|slurm-0001|1|no|1-infinite|idle|Unknown|19.05.0|1|16:1:1|0/1 |UNLIMITED |0/16/0/16 |1 |none |0/1/0/1 |(null) |Unknown |1 |n/a |OFF |slurm-0001 |0.01 |broad* |broad |all |idle |Unknown |N/A |16 |1 |1 \n"
                        + "up|(null)|48|0|110272|(null)|all|NO|infinite|257419|dragen01|dragen01|1|no|1-infinite|idle|Unknown|19.05.0|1|48:1:1|0/1 |UNLIMITED |0/48/0/48 |1 |none |0/1/0/1 |(null) |Unknown |1 |n/a |OFF |dragen01 |0.01 |dragen |dragen |all |idle |Unknown |N/A |48 |1 |1 ";
        ProcessOutput processOutput = new ProcessOutput(output.getBytes());
        ProcessResult processResult = new ProcessResult(0, processOutput);
        when(shellUtils.runSyncProcess(eq(cmd), any())).thenReturn(processResult);
        List<PartitionInfo> partitionInfos = slurmController.listPartitions();
        Assert.assertEquals(partitionInfos.size(), 2);
        PartitionInfo partitionInfo = partitionInfos.get(0);
        Assert.assertEquals(partitionInfo.getAvailable(), "up");
        Assert.assertEquals(partitionInfo.getName(), "broad* ");
        Assert.assertEquals(partitionInfo.getNodes(), 4);
        Assert.assertEquals(partitionInfo.getState(), "idle");
        Assert.assertEquals(partitionInfo.getNodeList(), "slurm-[0001-0002,0004-0005]");
    }

    @Test
    public void testListQueue() throws Exception {
        List<String> cmd = Arrays.asList("squeue", "-o", "\"%all\"");
        String output = "ACCOUNT|TRES_PER_NODE|MIN_CPUS|MIN_TMP_DISK|END_TIME|FEATURES|GROUP|OVER_SUBSCRIBE|JOBID|NAME|COMMENT|TIME_LIMIT|MIN_MEMORY|REQ_NODES|COMMAND|PRIORITY|QOS|REASON||ST|USER|RESERVATION|WCKEY|EXC_NODES|NICE|S:C:T|JOBID|EXEC_HOST|CPUS|NODES|DEPENDENCY|ARRAY_JOB_ID|GROUP|SOCKETS_PER_NODE|CORES_PER_SOCKET|THREADS_PER_CORE|ARRAY_TASK_ID|TIME_LEFT|TIME|NODELIST|CONTIGUOUS|PARTITION|PRIORITY|NODELIST(REASON)|START_TIME|STATE|UID|SUBMIT_TIME|LICENSES|CORE_SPEC|SCHEDNODES|WORK_DIR\n"
                        + "broad|N/A|1|0|NONE|(null)|broad|OK|37|Hello.sh|(null)|UNLIMITED|1G||./tests/Hello.sh|0.99998474074527|normal|Job's account not permitted to use this partition (dragen allows gpdragen not broad)||PD|jowalsh|(null)|(null)||0|*:*:*|37|n/a|1|1||37|1015|*|*|*|N/A|UNLIMITED|0:00||0|dragen|4294901757|(Job's account not permitted to use this partition (dragen allows gpdragen not broad))|2019-07-02T12:51:29|PENDING|10925|2019-06-13T10:13:08|(null)|N/A|dragen01|/home/unix/alosada";
        ProcessOutput processOutput = new ProcessOutput(output.getBytes());
        ProcessResult processResult = new ProcessResult(0, processOutput);
        when(shellUtils.runSyncProcess(eq(cmd), any())).thenReturn(processResult);
        List<QueueInfo> queueInfos = slurmController.listQueue();
        Assert.assertEquals(queueInfos.size(), 1);
        QueueInfo qInfo = queueInfos.get(0);
        Assert.assertEquals(qInfo.getName(), "Hello.sh");
        Assert.assertEquals(qInfo.getUser(), "jowalsh");
        Assert.assertEquals(qInfo.getJobId(), 37);
        Assert.assertEquals(qInfo.getNodes(), 1);
        Assert.assertEquals(qInfo.getState(), "PD");
    }

    @Test
    public void testViewJob() throws Exception {
        List<String> cmd = Arrays.asList("sacct", "-j", "89", "-p");
        String output = "JobID|JobName|Partition|Account|AllocCPUS|State|ExitCode|\n"
                        + "89|HFJNJDSXX_AlignTest|dragen|gpdragen|1|FAILED|127:0|\n"
                        + "89.batch|batch||gpdragen|1|FAILED|127:0|";
        ProcessOutput processOutput = new ProcessOutput(output.getBytes());
        ProcessResult processResult = new ProcessResult(0, processOutput);
        when(shellUtils.runSyncProcess(eq(cmd), any())).thenReturn(processResult);
        JobInfo jobInfo = slurmController.fetchJobInfo(89);
        Assert.assertNotNull(jobInfo);
        Assert.assertEquals(jobInfo.getName(), "HFJNJDSXX_AlignTest");
        Assert.assertEquals(jobInfo.getState(), "FAILED");
    }

    @Test
    public void testBatchJob() {
    }

    @Test
    public void testCancelJob() {
    }

    @Test
    public void testIsJobComplete() {
    }
}