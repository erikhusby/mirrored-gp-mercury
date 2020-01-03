package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AggregationTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AlignmentMetricsTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DragenTaskBuilder;
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.AlignmentStatsParser;
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.DragenReplayInfo;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AggregationState;
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
public class AggregationMetricsTaskHandler extends AbstractMetricsTaskHandler {

    private static final Log log = LogFactory.getLog(AggregationMetricsTaskHandler.class);

    @Override
    public void handleTask(Task task, SchedulerContext schedulerContext) {
        AlignmentMetricsTask alignmentMetricsTask = OrmUtil.proxySafeCast(task, AlignmentMetricsTask.class);

        State state = alignmentMetricsTask.getState();
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
        String commandLineArgument = aggregationTask.getCommandLineArgument();
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

            String outputFilePrefix =  DragenTaskBuilder.parseCommandFromArgument(
                    DragenTaskBuilder.OUTPUT_FILE_PREFIX, commandLineArgument);
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

            String runName = outputFilePrefix + "_" + outputDirectory.getName();
            Date runDate = new Date();
            String analysisname = outputDirectory.getName();
            String readGroup = outputFilePrefix + "_Aggregation";

            AlignmentStatsParser alignmentStatsParser = new AlignmentStatsParser();
            AlignmentStatsParser.AlignmentDataFiles alignmentDataFiles = alignmentStatsParser
                    .parseFolder(runName, runDate, analysisname, dragenReplayInfo, outputDirectory,
                            readGroup, outputFilePrefix);

            List<ProcessResult> processResults = new ArrayList<>();
            processResults.add(uploadMetric("/seq/lims/datawh/dev/dragen/mapping_run_metrics.ctl",
                    alignmentDataFiles.getMappingSummaryOutputFile(), alignmentDataFiles.getAlignSummaryLoad()));

            processResults.add(uploadMetric("/seq/lims/datawh/dev/dragen/mapping_rg_metrics.ctl",
                    alignmentDataFiles.getMappingMetricsOutputFile(), alignmentDataFiles.getAlignMetricLoad()));

            processResults.add(uploadMetric("/seq/lims/datawh/dev/dragen/variant_call_run_metrics.ctl",
                    alignmentDataFiles.getVcSummaryOutputFile(),  alignmentDataFiles.getVcSummaryMetricLoad()));

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
            String message = "Error processing alignment task metric " + alignmentMetricsTask;
            log.error(message, e);
            task.setErrorMessage(message);
            task.setStatus(Status.FAILED);
        }
    }
}
