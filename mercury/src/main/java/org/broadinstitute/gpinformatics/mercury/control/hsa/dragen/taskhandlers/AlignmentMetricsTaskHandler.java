package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

import com.opencsv.CSVReader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AlignmentMetricsTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AlignmentTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DragenTaskBuilder;
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.AlignmentStatsParser;
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.DemultiplexStatsParser;
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.DragenReplayInfo;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.ShellUtils;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AggregationState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AlignmentState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.zeroturnaround.exec.ProcessResult;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Dependent
public class AlignmentMetricsTaskHandler extends AbstractMetricsTaskHandler {

    private static final Log log = LogFactory.getLog(AlignmentMetricsTaskHandler.class);

    // TODO update if base file location changes
    private static final Pattern RUN_NAME_PATTERN =
            Pattern.compile("/seq/illumina/proc/SL-[A-Z]{3}/(.*)/dragen/(.*)/fastq.*");

    @Override
    public void handleTask(Task task, SchedulerContext schedulerContext) {
        AlignmentMetricsTask alignmentMetricsTask = OrmUtil.proxySafeCast(task, AlignmentMetricsTask.class);

        State state = alignmentMetricsTask.getState();
        if (!OrmUtil.proxySafeIsInstance(state, AlignmentState.class)) {
            throw new RuntimeException("Expect only an alignment state for an alignment metrics task.");
        }

        List<AlignmentTask> alignmentTasks = state.getTasks().stream()
                .filter(t -> t.getStatus() == Status.COMPLETE && OrmUtil.proxySafeIsInstance(t, AlignmentTask.class))
                .map(t -> OrmUtil.proxySafeCast(t, AlignmentTask.class))
                .collect(Collectors.toList());

        MessageCollection messageCollection = new MessageCollection();
        boolean failed = false;
        for (AlignmentTask alignmentTask: alignmentTasks) {
            try {
                String commandLineArgument = alignmentTask.getCommandLineArgument();
                String outputDirectoryPath = alignmentTask.getOutputDir().getPath();
                parse(alignmentTask, alignmentMetricsTask, outputDirectoryPath, commandLineArgument, messageCollection);
                if (alignmentMetricsTask.getStatus() != Status.COMPLETE) {
                    failed = true;
                }
            } catch (Exception e) {
                String message = "Error processing alignment task metric " + alignmentMetricsTask;
                log.error(message, e);
                alignmentMetricsTask.setErrorMessage(message);
                alignmentMetricsTask.setStatus(Status.FAILED);
            }
        }

        if (failed) {
            task.setStatus(Status.FAILED);
        }
    }

    public void parse(Task task, AlignmentMetricsTask alignmentMetricsTask, String outputDirectoryPath,
                      String commandLineArgument, MessageCollection messageCollection)
            throws IOException, TimeoutException, InterruptedException {
        if (outputDirectoryPath == null) {
            alignmentMetricsTask.setErrorMessage("Failed to parse output directory from task " + task);
            alignmentMetricsTask.setStatus(Status.SUSPENDED);
            return;
        }

        File outputDirectory = new File(outputDirectoryPath);
        if (!outputDirectory.exists()) {
            alignmentMetricsTask.setErrorMessage(
                    "Output directory for task " + task + " doesn't exist" + outputDirectory);
            alignmentMetricsTask.setStatus(Status.SUSPENDED);
            return;
        }

        Pair<String, String> runNameDatePair = parseRunNameAndAnalysisDateFromOutputDir(outputDirectory);
        if (runNameDatePair == null) {
            alignmentMetricsTask.setErrorMessage("Failed to parse run name and date from output " + outputDirectory.getPath());
            alignmentMetricsTask.setStatus(Status.SUSPENDED);
            return;
        }

        String outputFilePrefix =  DragenTaskBuilder.parseCommandFromArgument(
                DragenTaskBuilder.OUTPUT_FILE_PREFIX, commandLineArgument);
        if (outputFilePrefix == null) {
            alignmentMetricsTask.setErrorMessage("Failed to parse output file prefix from task " + task);
            alignmentMetricsTask.setStatus(Status.SUSPENDED);
            return;
        }

        String fastQFilePath = DragenTaskBuilder.parseCommandFromArgument(
                DragenTaskBuilder.FASTQ_LIST, commandLineArgument);
        if (fastQFilePath == null) {
            alignmentMetricsTask.setErrorMessage("Failed to parse fastq list file from task " + task);
            alignmentMetricsTask.setStatus(Status.SUSPENDED);
            return;
        }
        File fastQFile = new File(fastQFilePath);
        if (!fastQFile.exists()) {
            alignmentMetricsTask.setErrorMessage("Fastq list file for task " + task + " doesn't exist" + outputDirectory);
            alignmentMetricsTask.setStatus(Status.SUSPENDED);
            return;
        }


        File replayJsonFile = new File(outputDirectory, outputFilePrefix + "-replay.json");
        DragenReplayInfo dragenReplayInfo = parseReplay(replayJsonFile, messageCollection);
        if (dragenReplayInfo == null) {
            String errMsg = "Failed to read replay file " + replayJsonFile.getPath();
            alignmentMetricsTask.setErrorMessage(errMsg);
            alignmentMetricsTask.setStatus(Status.SUSPENDED);
            return;
        }

        String runName = runNameDatePair.getLeft();
        String analysisname = runNameDatePair.getRight();
        AlignmentStatsParser alignmentStatsParser = new AlignmentStatsParser();
        AlignmentStatsParser.AlignmentDataFiles alignmentDataFiles = alignmentStatsParser
                .parseFolder(runName, new Date(), analysisname, dragenReplayInfo, outputDirectory,
                        outputFilePrefix, outputFilePrefix);

        if (messageCollection.hasErrors()) {
            alignmentMetricsTask.setStatus(Status.SUSPENDED);
            return;
        }

        List<ProcessResult> processResults = new ArrayList<>();
        processResults.add(uploadMetric("/seq/lims/datawh/dev/dragen/mapping_run_metrics.ctl",
                alignmentDataFiles.getMappingSummaryOutputFile(), alignmentDataFiles.getAlignSummaryLoad()));

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

        alignmentMetricsTask.setStatus(failed ? Status.FAILED : Status.COMPLETE);
    }

    private void doCleanup(File outputDirectory) {
        try {
            FileUtils.deleteDirectory(outputDirectory);
        } catch (IOException e) {
            log.error("Error cleaning up alignment files", e);
        }
    }

    protected Pair<String, String> parseRunNameAndAnalysisDateFromOutputDir(File outputDirectory) {
        Matcher matcher = RUN_NAME_PATTERN.matcher(outputDirectory.getPath());
        if (matcher.matches()) {
            String runName = matcher.group(1);
            String date = matcher.group(2);
            return Pair.of(runName, date);
        }
        return null;
    }


    public Map<String, String> buildMapOfReadGroupToSampleAlias(File fastQFile) throws IOException {
        Map<String, String> mapReadGroupToSampleAliasFromFastQ = new HashMap<>();
        CSVReader csvReader = new CSVReader(new BufferedReader(new FileReader(fastQFile)));
        String[] nextRecord;
        while ((nextRecord = csvReader.readNext()) != null) {
            String rgId = nextRecord[0];
            String sampleAlias = nextRecord[1];
            mapReadGroupToSampleAliasFromFastQ.put(rgId, sampleAlias);
        }
        return mapReadGroupToSampleAliasFromFastQ;
    }
}
