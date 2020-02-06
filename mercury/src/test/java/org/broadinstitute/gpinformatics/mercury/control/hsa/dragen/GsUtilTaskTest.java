package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.broadinstitute.gpinformatics.infrastructure.deployment.DragenConfig;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.ShellUtils;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SlurmController;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.codehaus.plexus.util.FileUtils;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.zeroturnaround.exec.ProcessOutput;
import org.zeroturnaround.exec.ProcessResult;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.Matchers.contains;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Test(groups = TestGroups.DATABASE_FREE)
public class GsUtilTaskTest {

    @Test
    public void testCp() {
        File tmpCram = FileUtils.createTempFile("test", "cram", null);
        GsUtilTask task = GsUtilTask.cp(tmpCram, "gs://broad-gplims-dev");
        String expected = String.format(
                "gsutil -o GSUtil:parallel_process_count=1 -o GSUtil:parallel_thread_count=4 -o GSUtil:parallel_composite_upload_threshold=150M cp %s gs://broad-gplims-dev",
                tmpCram.getPath());
        Assert.assertEquals(task.getCommandLineArgument(), expected);
    }

    @Test
    public void testSlurmControllerGsutil() throws InterruptedException, IOException, TimeoutException {
        GsUtilTask task = GsUtilTask.cp(new File("crama.cram"), "gs://broad-gplims-dev");
        SlurmController slurmController = new SlurmController();
        DragenConfig dragenConfig = new DragenConfig(org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV);
        slurmController.setDragenConfig(dragenConfig);
        ShellUtils mockShell = mock(ShellUtils.class);
        String created_pid = "209";
        ProcessOutput output = new ProcessOutput(("batch job " + created_pid).getBytes());
        when(mockShell.runSyncProcess(anyList())).thenReturn(new ProcessResult(0, output));
        slurmController.setShellUtils(mockShell);
        System.out.println(task.getPartition());
        String pid = slurmController.batchJob(task.getPartition(), OrmUtil.proxySafeCast(task, ProcessTask.class));
        verify(mockShell).runSyncProcess(org.broadinstitute.gpinformatics.Matchers.argThat(contains(
                "ssh", "login04", "sbatch", "--exclusive", "-J", "Cp_crama.cram", "-p", "dragen_cpu", "--cpus-per-task=4",
                "--wrap=\"/seq/techdev/software/infrastructure/wildfly/gcloud-auth.sh && "
                + "/broad/software/free/Linux/redhat_7_x86_64/pkgs/google-cloud-sdk/bin/gsutil -o "
                + "GSUtil:parallel_process_count=1 -o GSUtil:parallel_thread_count=4 -o "
                + "GSUtil:parallel_composite_upload_threshold=150M cp crama.cram gs://broad-gplims-dev\""
        )));
        Assert.assertEquals("209", pid);
    }
}