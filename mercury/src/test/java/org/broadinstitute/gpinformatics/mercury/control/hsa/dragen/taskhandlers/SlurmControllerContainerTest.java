package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.DragenConfig;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.TaskDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.ProcessTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.ShellUtils;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SlurmController;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.mockito.ArgumentCaptor;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.zeroturnaround.exec.ProcessOutput;
import org.zeroturnaround.exec.ProcessResult;

import javax.inject.Inject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verify's the expected input to slurm for various tasks such as aggregation and gsutil tasks
 */
@Test(groups = TestGroups.STANDARD)
public class SlurmControllerContainerTest extends Arquillian {

    @Inject
    private TaskDao taskDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "prod");
    }

    @Test
    public void testSlurmControllerAggregation() throws InterruptedException, IOException, TimeoutException {
        Task task = taskDao.findTaskById(19000L);
        String cmd = runTask(task);
        String expected = "ssh login04 sbatch -J Agg_SM-HJWQX --output /seq/dragen/log/slurm-%j.out -p dragen "
                          + "--exclusive --wrap=\"/opt/edico/bin/dragen_reset && /opt/edico/bin/dragen "
                          + "-f -r /seq/dragen/references/hg38/current --fastq-list "
                          + "/seq/dragen/aggregation/SM-HJWQX/20-01-14_10-47-29/fastq_list.csv "
                          + "--fastq-list-sample-id SM-HJWQX "
                          + "--output-directory /seq/dragen/aggregation/SM-HJWQX/20-01-14_10-47-29 "
                          + "--intermediate-results-dir /local/scratch "
                          + "--output-file-prefix SM-HJWQX "
                          + "--vc-sample-name SM-HJWQX "
                          + "--enable-variant-caller true "
                          + "--enable-duplicate-marking true "
                          + "--enable-map-align-output true "
                          + "--output-format=CRAM "
                          + "--qc-cross-cont-vcf /opt/edico/config/sample_cross_contamination_resource_hg38.vcf "
                          + "--qc-coverage-region-1 /seq/dragen/references/hg38/current/wgs_coverage_regions.hg38.interval_list.bed "
                          + "--qc-coverage-reports-1 cov_report \"";
        Assert.assertEquals(cmd, expected);
    }

    /**
     * GSUtil jobs are different in that they aren't exclusive and have a set cpu core count
     */
    @Test
    public void testSlurmControllerGsutil() throws InterruptedException, IOException, TimeoutException {
        Task task = taskDao.findTaskById(18552L);
        String cmd = runTask(task);
        String expected = "ssh login04 sbatch -J Cp_SM-JHOKJ.cram --output /seq/dragen/log/slurm-%j.out -p dragen --cpus-per-task=4 "
                          + "--wrap=\"/seq/techdev/software/infrastructure/wildfly/gcloud-auth.sh && gsutil -o GSUtil:parallel_process_count=1 "
                          + "-o GSUtil:parallel_thread_count=4 -o GSUtil:parallel_composite_upload_threshold=150M "
                          + "cp /seq/dragen/aggregation/SM-JHOKJ/19-12-12_10-53-21/SM-JHOKJ.cram gs://broad-gplims-dev\"";
        Assert.assertEquals(cmd, expected);
    }

    private String runTask(Task task) throws InterruptedException, TimeoutException, IOException {
        SlurmController slurmController = new SlurmController();
        ShellUtils shellUtils = mock(ShellUtils.class);
        slurmController.setShellUtils(shellUtils);
        ProcessResult result = new ProcessResult(0, new ProcessOutput("Submitted batch job 19000".getBytes()));
        when(shellUtils.runSyncProcess(anyList())).thenReturn(result);
        DragenConfig dragenConfig =
                new DragenConfig(org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV);
        slurmController.setDragenConfig(dragenConfig);
        slurmController.batchJob("dragen", OrmUtil.proxySafeCast(task, ProcessTask.class));

        ArgumentCaptor<List> argument = ArgumentCaptor.forClass(List.class);
        verify(shellUtils).runSyncProcess(argument.capture());
        List<String> values = argument.getValue();
        return StringUtils.join(values, ' ');
    }

}