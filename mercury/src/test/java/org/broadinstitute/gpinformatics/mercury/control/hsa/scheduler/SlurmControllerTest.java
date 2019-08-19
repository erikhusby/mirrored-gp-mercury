package org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.DragenConfig;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.ProcessTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.zeroturnaround.exec.ProcessOutput;
import org.zeroturnaround.exec.ProcessResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class SlurmControllerTest {

    private SlurmController slurmController;

    @BeforeMethod
    public void setUp() {
        slurmController = new SlurmController();
        slurmController.setShellUtils(new ShellUtils());
        DragenConfig dragenConfig = new DragenConfig(Deployment.DEV);
        slurmController.setDragenConfig(dragenConfig);
    }

    @Test
    public void testSshAcct() {
        List<PartitionInfo> partitionInfos = slurmController.listPartitions();
        Assert.assertNotNull(partitionInfos);
        Assert.assertEquals(true, partitionInfos.size() > 0);
    }

    @Test
    public void testSshQueue() {
        List<QueueInfo> queueInfos = slurmController.listQueue();
        Assert.assertNotNull(queueInfos);
        Assert.assertEquals(true, queueInfos.size() > 0);
    }

    @Test
    public void checkErroredJobStatus() {
        Status status = slurmController.fetchJobStatus(100L);
        Assert.assertEquals(Status.FAILED, status);
    }

    @Test
    public void checkCompletedJobStatus() {
        Status status = slurmController.fetchJobStatus(125L);
        Assert.assertEquals(Status.COMPLETE, status);
    }

    @Test
    public void tempTest() {
        JobInfo status = slurmController.fetchJobInfo(148L);
        System.out.println(status);
        slurmController.cancelJob("149");
    }

    @Test
    public void testBatch() {
        ProcessTask processTask = new ProcessTask();
        processTask.setTaskName("JWTestsSlurmFromSSH1");
        processTask.setCommandLineArgument("dragen -f -r /staging/reference/hg38/v1/ --fastq-list /seq/dragen/bcl2fastq/190508_A00764_0025_AHFJNJDSXX/fastq/Reports/fastq_list.csv --fastq-list-sample-id TCGA-ZF-A9RE-01A-11D-A38G-08 --output-directory /seq/dragen/bcl2fastq/190508_A00764_0025_AHFJNJDSXX/fastq/TCGA-ZF-A9RE-01A-11D-A38G-08/ --intermediate-results-dir /staging/out/ --output-file-prefix TCGA-ZF-A9RE-01A-11D-A38G-08 --vc-sample-name TTCGA-ZF-A9RE-01A-11D-A38G-08 --enable-variant-caller true --enable-duplicate-marking true --enable-map-align-output true");
        String jobId = slurmController.batchJob("dragen", processTask);
        slurmController.fetchJobStatus(Long.parseLong(jobId));
    }
}