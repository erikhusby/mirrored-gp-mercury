package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AggregationTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AlignmentMetricsTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.AlignmentStatsParser;
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.DragenReplayInfo;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AggregationState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.ReadGroupUtil;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.zeroturnaround.exec.ProcessResult;

import javax.enterprise.context.Dependent;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Dependent
public class AggregationMetricsTaskHandler extends AbstractMetricsTaskHandler<AlignmentMetricsTask>{

    private static final Log log = LogFactory.getLog(AggregationMetricsTaskHandler.class);

    @Override
    public void handleTask(AlignmentMetricsTask task, SchedulerContext schedulerContext) {
        State state = task.getState();
        if (!OrmUtil.proxySafeIsInstance(state, AggregationState.class)) {
            throw new RuntimeException("Expect only an Aggregation state for an alignment metrics task.");
        }

        List<AggregationTask> tasks = state.getTasks().stream()
                .filter(t -> t.getStatus() == Status.COMPLETE && OrmUtil.proxySafeIsInstance(t, AggregationTask.class))
                .map(t -> OrmUtil.proxySafeCast(t, AggregationTask.class))
                .collect(Collectors.toList());

        if (tasks.size() != 1) {
            throw new RuntimeException("Expect only one aggregation task per state");
        }

        AggregationTask aggregationTask = tasks.iterator().next();
        String outputDirectoryPath = aggregationTask.getOutputDir().getPath();

        MessageCollection messageCollection = new MessageCollection();
        try {
            if (outputDirectoryPath == null) {
                task.setErrorMessage("Failed to parse output directory from task " + task);
                task.setStatus(Status.FAILED);
                return;
            }

            File outputDirectory = new File(outputDirectoryPath);
            if (!outputDirectory.exists()) {
                task.setErrorMessage(
                        "Output directory for task " + task + " doesn't exist" + outputDirectory);
                task.setStatus(Status.FAILED);
                return;
            }

            String outputFilePrefix =  aggregationTask.getOutputFilePrefix();
            if (outputFilePrefix == null) {
                task.setErrorMessage("Failed to parse output file prefix from task " + task);
                task.setStatus(Status.FAILED);
                return;
            }

            File replayJsonFile = new File(outputDirectory, outputFilePrefix + "-replay.json");
            DragenReplayInfo dragenReplayInfo = parseReplay(replayJsonFile, messageCollection);
            if (dragenReplayInfo == null) {
                String errMsg = "Failed to read replay file " + replayJsonFile.getPath();
                task.setErrorMessage(errMsg);
                task.setStatus(Status.FAILED);
                return;
            }

            if (messageCollection.hasErrors()) {
                task.setStatus(Status.FAILED);
                return;
            }

            String sampleName =  aggregationTask.getFastQSampleId();
            String runName = sampleName + "_" + outputDirectory.getName();
            Date runDate = new Date();
            String analysisname = outputDirectory.getName();
            String readGroup = ReadGroupUtil.toAggregationReadGroupMetric(sampleName);
            boolean containsContamination = aggregationTask.getQcContaminationFile() != null;
            AlignmentStatsParser alignmentStatsParser = new AlignmentStatsParser();
            AlignmentStatsParser.AlignmentDataFiles alignmentDataFiles = alignmentStatsParser
                    .parseFolder(runName, runDate, analysisname, dragenReplayInfo, outputDirectory,
                            readGroup, outputFilePrefix, containsContamination);

            List<ProcessResult> processResults = new ArrayList<>();
            processResults.add(uploadMetric(getCtlFilePath("mapping_run_metrics.ctl"),
                    alignmentDataFiles.getMappingSummaryOutputFile(), getLogPath(alignmentDataFiles.getAlignSummaryLoad())));

            processResults.add(uploadMetric(getCtlFilePath("mapping_rg_metrics.ctl"),
                    alignmentDataFiles.getMappingMetricsOutputFile(), getLogPath(alignmentDataFiles.getAlignMetricLoad())));

            // TODO Only if vc enabled
            processResults.add(uploadMetric(getCtlFilePath("variant_call_run_metrics.ctl"),
                    alignmentDataFiles.getVcSummaryOutputFile(),  getLogPath(alignmentDataFiles.getVcSummaryMetricLoad())));

            boolean failed = false;
            for (ProcessResult processResult : processResults) {
                if (processResult.getExitValue() != 0) {
                    failed = true;
                    if (processResult.hasOutput()) {
                        log.info(processResult.getOutput().getString());
                    }
                }
            }

            task.setStatus(failed ? Status.FAILED : Status.COMPLETE);

        } catch (Exception e) {
            String message = "Error processing alignment task metric " + task;
            log.error(message, e);
            task.setErrorMessage(message);
            task.setStatus(Status.FAILED);
        }
    }
}
