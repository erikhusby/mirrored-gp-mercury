package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

import com.opencsv.CSVReader;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Dependent
public class AlignmentMetricsTaskHandler extends AbstractTaskHandler {

    private static final Log log = LogFactory.getLog(AlignmentMetricsTaskHandler.class);

    private static final Pattern RUN_NAME_PATTERN =
            Pattern.compile("/seq/illumina/proc/SL-[A-Z]{3}/(.*)/dragen/(.*)/fastq/.*");

    @Inject
    private ShellUtils shellUtils;

    @Override
    public void handleTask(Task task, SchedulerContext schedulerContext) {
        AlignmentMetricsTask alignmentMetricsTask = OrmUtil.proxySafeCast(task, AlignmentMetricsTask.class);

        State state = alignmentMetricsTask.getState();
        if (!OrmUtil.proxySafeIsInstance(state, AlignmentState.class)) {
            throw new RuntimeException("Expect only a alignment state for an alignment metrics task.");
        }
        AlignmentState alignmentState = OrmUtil.proxySafeCast(state, AlignmentState.class);

        List<AlignmentTask> alignmentTasks = alignmentState.getTasks().stream()
                .filter(t -> t.getStatus() == Status.COMPLETE && OrmUtil.proxySafeIsInstance(t, AlignmentTask.class))
                .map(t -> OrmUtil.proxySafeCast(t, AlignmentTask.class))
                .collect(Collectors.toList());

        MessageCollection messageCollection = new MessageCollection();
        for (AlignmentTask alignmentTask: alignmentTasks) {
            try {
                // TODO needs to be 4 separate tasks to handle retries
                String commandLineArgument = alignmentTask.getCommandLineArgument();
                String outputDirectoryPath = alignmentTask.getOutputDir().getPath();
                if (outputDirectoryPath == null) {
                    task.setErrorMessage("Failed to parse output directory from task " + alignmentTask);
                    task.setStatus(Status.FAILED);
                    continue;
                }

                File outputDirectory = new File(outputDirectoryPath);
                if (!outputDirectory.exists()) {
                    task.setErrorMessage(
                            "Output directory for task " + alignmentTask + " doesn't exist" + outputDirectory);
                    task.setStatus(Status.FAILED);
                    continue;
                }

                Pair<String, String> runNameDatePair = parseRunNameAndAnalysisDateFromOutputDir(outputDirectory);
                if (runNameDatePair == null) {
                    task.setErrorMessage("Failed to parse run name and date from output " + outputDirectory.getPath());
                    task.setStatus(Status.FAILED);
                    continue;
                }

                String outputFilePrefix =
                        DragenTaskBuilder
                                .parseCommandFromArgument(DragenTaskBuilder.OUTPUT_FILE_PREFIX, commandLineArgument);
                if (outputFilePrefix == null) {
                    task.setErrorMessage("Failed to parse output file prefix from task " + alignmentTask);
                    task.setStatus(Status.FAILED);
                    continue;
                }

                String fastQFilePath =
                        DragenTaskBuilder.parseCommandFromArgument(DragenTaskBuilder.FASTQ_LIST, commandLineArgument);
                if (fastQFilePath == null) {
                    task.setErrorMessage("Failed to parse fastq list file from task " + alignmentTask);
                    task.setStatus(Status.FAILED);
                    continue;
                }
                File fastQFile = new File(fastQFilePath);
                if (!fastQFile.exists()) {
                    task.setErrorMessage("Fastq list file for task " + alignmentTask + " doesn't exist" + outputDirectory);
                    task.setStatus(Status.FAILED);
                    continue;
                }

                File replayJsonFile = new File(outputDirectory, outputFilePrefix + "-replay.json");
                DragenReplayInfo dragenReplayInfo = null;
                try (InputStream inputStream = new FileInputStream(replayJsonFile)) {
                    dragenReplayInfo = new DemultiplexStatsParser().parseReplayInfo(inputStream, messageCollection);
                } catch (IOException e) {
                    String errMsg = "Failed to read replay file " + replayJsonFile.getPath();
                    log.error(errMsg, e);
                    task.setErrorMessage(errMsg);
                    task.setStatus(Status.FAILED);
                    continue;
                }

                Map<String, String> mapReadGroupToSample = buildMapOfReadGroupToSampleAlias(fastQFile);

                AlignmentStatsParser alignmentStatsParser = new AlignmentStatsParser();
                AlignmentStatsParser.AlignmentDataFiles alignmentDataFiles = alignmentStatsParser
                        .parseStats(outputDirectory, outputFilePrefix, dragenReplayInfo, messageCollection,
                                mapReadGroupToSample, runNameDatePair);

                List<ProcessResult> processResults = new ArrayList<>();

                if (!messageCollection.hasErrors()) {
                    processResults.add(uploadMetric("/seq/lims/datawh/dev/dragen/mapping_run_metrics.ctl",
                            alignmentDataFiles.getMappingSummaryOutputFile()));
                    processResults.add(uploadMetric("/seq/lims/datawh/dev/dragen/mapping_rg_metrics.ctl",
                            alignmentDataFiles.getMappingMetricsOutputFile()));
                    processResults.add(uploadMetric("/seq/lims/datawh/dev/dragen/variant_call_run_metrics.ctl",
                            alignmentDataFiles.getVcSummaryOutputFile()));
                    processResults.add(uploadMetric("/seq/lims/datawh/dev/dragen/variant_call_metrics.ctl",
                            alignmentDataFiles.getVcMetricsOutputFile()));
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
                } else {
                    task.setStatus(Status.FAILED);
                }

            } catch (Exception e) {
                String message = "Error processing alignment task metric " + alignmentMetricsTask;
                log.error(message, e);
                task.setErrorMessage(message);
                task.setStatus(Status.FAILED);
            }
        }
    }

    // TODO FAils simulator
    private Pair<String, String> parseRunNameAndAnalysisDateFromOutputDir(File outputDirectory) {
        Matcher matcher = RUN_NAME_PATTERN.matcher(outputDirectory.getPath());
        if (matcher.matches()) {
            String runName = matcher.group(1);
            String date = matcher.group(2);
            return Pair.of(runName, date);
        }
        return null;
    }

    // TODO share between both tasks
    private ProcessResult uploadMetric(String ctlFilePath, File dataPath)
            throws IOException, TimeoutException, InterruptedException {
        String ldruid = "mercurydw/seq_dev3@\"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=seqdev.broad.mit.edu)(PORT=1521)))(CONNECT_DATA=(SERVER=DEDICATED)(SID=seqdev3)))\"";
        List<String> cmds = Arrays.asList("sqlldr",
                String.format("control=%s", ctlFilePath),
                "log=load.log",
                "bad=load.bad",
                String.format("data=%s", dataPath.getPath()),
                "discard=load.dsc",
                "direct=false",
                String.format("userId=%s", ldruid));
        return shellUtils.runSyncProcess(cmds);
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

    public void setShellUtils(ShellUtils shellUtils) {
        this.shellUtils = shellUtils;
    }
}
