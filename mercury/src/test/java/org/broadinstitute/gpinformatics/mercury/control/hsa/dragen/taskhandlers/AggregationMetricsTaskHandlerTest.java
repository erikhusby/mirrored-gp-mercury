package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.DragenConfig;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AggregationTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AlignmentMetricsTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DragenFolderUtil;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.ShellUtils;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AggregationState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.presentation.hsa.AggregationActionBean;
import org.jboss.threads.AsyncFuture;
import org.testng.annotations.Test;
import org.zeroturnaround.exec.ProcessOutput;
import org.zeroturnaround.exec.ProcessResult;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

@Test(groups = TestGroups.DATABASE_FREE)
public class AggregationMetricsTaskHandlerTest {

    @Test
    public void testHandleTask() throws InterruptedException, IOException, TimeoutException {
        AggregationMetricsTaskHandler handler = new AggregationMetricsTaskHandler();

        DragenConfig dragenConfig = new DragenConfig(Deployment.DEV);

        File referenceFile = new File(AggregationActionBean.ReferenceGenome.HG38.getPath());
        File outputDir = new File("/seq/dragen/aggregation/SM-JH386/19-10-07_03-18-44");
        File fastQList = new File(outputDir, DragenFolderUtil.FASTQ_LIST_CSV);
        File intermediateResults = new File(dragenConfig.getIntermediateResults());
        File contaminationFile = new File(AggregationActionBean.ReferenceGenome.HG38.getContamFile());
        File bedFile = new File(AggregationActionBean.ReferenceGenome.HG38.getCoverageBedFile());
        File configFile = new File("/seq/dragen/config/test.cfg");

        AggregationTask aggregationTask = new AggregationTask(referenceFile, fastQList, "SM-JH386",
                outputDir, intermediateResults, "SM-JH386", "SM-JH386", contaminationFile, bedFile, null, null,
                configFile);
        aggregationTask.setStatus(Status.COMPLETE);

        AggregationState state = new AggregationState();
        state.addTask(aggregationTask);

        AlignmentMetricsTask metricsTask = new AlignmentMetricsTask();
        state.addExitTask(metricsTask);

        ShellUtils shellUtils = mock(ShellUtils.class);
        when(shellUtils.runSyncProcess(any())).thenReturn(new ProcessResult(0, new ProcessOutput("Done".getBytes())));
        handler.setShellUtils(shellUtils);
        handler.setDragenConfig(dragenConfig);
        handler.handleTask(metricsTask, null);
    }
}