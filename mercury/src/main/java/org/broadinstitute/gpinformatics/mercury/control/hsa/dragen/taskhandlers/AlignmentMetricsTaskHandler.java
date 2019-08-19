package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

import com.opencsv.CSVReader;
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
import java.util.stream.Collectors;

@Dependent
public class AlignmentMetricsTaskHandler extends AbstractTaskHandler {

    private static final Log log = LogFactory.getLog(AlignmentMetricsTaskHandler.class);

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
                String commandLineArgument = alignmentTask.getCommandLineArgument();
                String outputDirectoryPath = alignmentTask.getOutputDir().getPath();
                if (outputDirectoryPath == null) {
                    messageCollection.addError("Failed to parse output directory from task " + alignmentTask);
                    continue;
                }

                File outputDirectory = new File(outputDirectoryPath);
                if (!outputDirectory.exists()) {
                    messageCollection.addError(
                            "Output directory for task " + alignmentTask + " doesn't exist" + outputDirectory);
                    continue;
                }

                String outputFilePrefix =
                        DragenTaskBuilder
                                .parseCommandFromArgument(DragenTaskBuilder.OUTPUT_FILE_PREFIX, commandLineArgument);
                if (outputFilePrefix == null) {
                    messageCollection.addError("Failed to parse output file prefix from task " + alignmentTask);
                    continue;
                }

                String fastQFilePath =
                        DragenTaskBuilder.parseCommandFromArgument(DragenTaskBuilder.FASTQ_LIST, commandLineArgument);
                if (fastQFilePath == null) {
                    messageCollection.addError("Failed to parse fastq list file from task " + alignmentTask);
                    continue;
                }
                File fastQFile = new File(fastQFilePath);
                if (!fastQFile.exists()) {
                    messageCollection
                            .addError("Fastq list file for task " + alignmentTask + " doesn't exist" + outputDirectory);
                    continue;
                }

                File replayJsonFile = new File(outputDirectory, outputFilePrefix + "-replay.json");
                DragenReplayInfo dragenReplayInfo = null;
                try (InputStream inputStream = new FileInputStream(replayJsonFile)) {
                    dragenReplayInfo = new DemultiplexStatsParser().parseReplayInfo(inputStream, messageCollection);
                } catch (IOException e) {
                    String errMsg = "Failed to read replay file " + replayJsonFile.getPath();
                    log.error(errMsg, e);
                    messageCollection.addError(errMsg);
                }

                Map<String, String> mapReadGroupToSample = buildMapOfReadGroupToSampleAlias(fastQFile);

                AlignmentStatsParser alignmentStatsParser = new AlignmentStatsParser();
                AlignmentStatsParser.AlignmentDataFiles alignmentDataFiles = alignmentStatsParser
                        .parseStats(outputDirectory, outputFilePrefix, dragenReplayInfo, messageCollection,
                                mapReadGroupToSample);

                List<ProcessResult> processResults = new ArrayList<>();

                if (!messageCollection.hasErrors()) {
                    processResults.add(uploadMetric("/seq/lims/datawh/dev/new/mapping_run_metrics.ctl",
                            alignmentDataFiles.getMappingSummaryOutputFile()));
                    processResults.add(uploadMetric("/seq/lims/datawh/dev/new/mapping_rg_metrics.ctl",
                            alignmentDataFiles.getMappingMetricsOutputFile()));
                    processResults.add(uploadMetric("/seq/lims/datawh/dev/new/variant_call_run_metrics.ctl",
                            alignmentDataFiles.getVcSummaryOutputFile()));
                    processResults.add(uploadMetric("/seq/lims/datawh/dev/new/variant_call_metrics.ctl",
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

            } catch (IOException e) {
                String message = "I/O Error processing alignment task metric " + alignmentMetricsTask;
                messageCollection.addError(message);
                log.error(message, e);
                task.setStatus(Status.FAILED);
            } catch (InterruptedException e) {
                String message = "I/O Error processing alignment task metric " + alignmentMetricsTask;
                messageCollection.addError(message);
                log.error(message, e);
                task.setStatus(Status.FAILED);
            } catch (TimeoutException e) {
                String message = "I/O Error processing alignment task metric " + alignmentMetricsTask;
                messageCollection.addError(message);
                log.error(message, e);
                task.setStatus(Status.FAILED);
            }
        }
    }

    private ProcessResult uploadMetric(String ctlFilePath, File dataPath)
            throws IOException, TimeoutException, InterruptedException {
        String ldruid = "mercury/guest@\"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=192.168.194.136)(PORT=1521)))(CONNECT_DATA=(SERVER=DEDICATED)(SID=gpgold)))\"";
        List<String> cmds = Arrays.asList("/Users/jowalsh/opt/oracle/sqlldr",
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
