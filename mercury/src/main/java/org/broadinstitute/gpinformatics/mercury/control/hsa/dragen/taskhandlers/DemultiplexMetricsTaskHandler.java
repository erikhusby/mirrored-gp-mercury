package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.DemultiplexLaneMetric;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.DemultiplexSampleMetric;
import org.broadinstitute.gpinformatics.infrastructure.deployment.DragenConfig;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DemultiplexMetricsTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DemultiplexTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DragenFolderUtil;
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.DemultiplexStats;
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.DemultiplexStatsParser;
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.DragenReplayInfo;
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.MetricsRecordWriter;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.DemultiplexState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Dependent
public class DemultiplexMetricsTaskHandler extends BaseDemultiplexMetricsHandler {

    private static final Log log = LogFactory.getLog(DemultiplexMetricsTaskHandler.class);

    @Inject
    private DemultiplexStatsParser demultiplexStatsParser;

    @Inject
    private DragenConfig dragenConfig;

    // For testing
    private List<DemultiplexStats> demultiplexStats;

    private DragenReplayInfo dragenReplayInfo;

    @Override
    public void handleTask(Task task, SchedulerContext schedulerContext) {
        DemultiplexMetricsTask demultiplexMetricsTask = OrmUtil.proxySafeCast(task, DemultiplexMetricsTask.class);

        State state = demultiplexMetricsTask.getState();
        if (!OrmUtil.proxySafeIsInstance(state, DemultiplexState.class)) {
            throw new RuntimeException("Expect only a demultiplex state for a demultiplex metrics task.");
        }
        DemultiplexState demultiplexState = OrmUtil.proxySafeCast(state, DemultiplexState.class);
        List<DemultiplexTask> demultiplexTasks = demultiplexState.getDemultiplexTasks().stream()
                .filter(Task::isComplete)
                .collect(Collectors.toList());

        // TODO one DAT file
        boolean failed = false;
        for (DemultiplexTask demultiplexTask: demultiplexTasks) {
            File runDirectory = demultiplexTask.getBclInputDirectory();
            IlluminaSequencingRun run = findRunFromDirectory(runDirectory, demultiplexState.getSequencingRunChambers());
            if (run == null) {
                throw new RuntimeException("Failed to find run in demux state " + runDirectory.getPath() + " " + demultiplexState.getStateId());
            }
            uploadMetrics(task, run, demultiplexState);
            if (task.getStatus() != Status.COMPLETE) {
                failed = true;
            }
        }

        if (failed) {
            task.setStatus(Status.FAILED);
        }
    }

    protected void uploadMetrics(Task task, IlluminaSequencingRun run, DemultiplexState state) {
        DragenFolderUtil dragenFolderUtil = new DragenFolderUtil(dragenConfig, run, state.getStateName());
        File demultiplexMetricsFile = dragenFolderUtil.getDemultiplexStatsFile();
        if (!demultiplexMetricsFile.exists()) {
            String errMsg = "Demultiplex Metrics file doesn't exist: " + demultiplexMetricsFile.getPath();
            log.info(errMsg);
            task.setErrorMessage(errMsg);
            task.setStatus(Status.SUSPENDED);
            return;
        }

        MessageCollection messageCollection = new MessageCollection();
        demultiplexStats = new ArrayList<>();
        try(InputStream inputStream = new FileInputStream(demultiplexMetricsFile)) {
            demultiplexStats = demultiplexStatsParser.parseStats(inputStream, messageCollection);
        } catch (IOException e) {
            String errMsg = "Failed to read demultiplex stats file " + demultiplexMetricsFile.getPath();
            log.error(errMsg, e);
            task.setErrorMessage(errMsg);
            task.setStatus(Status.SUSPENDED);
            return;
        }

        File replayJsonFile = dragenFolderUtil.getReplayJsonFile();
        try(InputStream inputStream = new FileInputStream(replayJsonFile)) {
            dragenReplayInfo = demultiplexStatsParser.parseReplayInfo(inputStream, messageCollection);
        } catch (IOException e) {
            String errMsg = "Failed to read replay file " + replayJsonFile.getPath();
            log.error(errMsg, e);
            task.setErrorMessage(errMsg);
            task.setStatus(Status.SUSPENDED);
            return;
        }

        Pair<List<DemultiplexSampleMetric>, List<DemultiplexLaneMetric>> pair =
                createSequencingRunMetricDaoFree(state, run, messageCollection, dragenReplayInfo, demultiplexStats);
        List<DemultiplexSampleMetric> sequencingMetricRun = pair.getLeft();
        List<DemultiplexLaneMetric> laneMetrics = pair.getRight();

        if (!messageCollection.hasErrors()) {
            uploadResults(task, run, dragenFolderUtil.getReportsFolder(), sequencingMetricRun, laneMetrics);
        } else {
            log.error("Errors processing demultiplex stats" +
                      StringUtils.join(messageCollection.getErrors(), ","));
        }
    }

    public List<DemultiplexStats> getDemultiplexStats() {
        return demultiplexStats;
    }

    public DragenReplayInfo getDragenReplayInfo() {
        return dragenReplayInfo;
    }

    // For Testing
    @Override
    public void setDragenConfig(DragenConfig dragenConfig) {
        this.dragenConfig = dragenConfig;
    }

    public void setDemultiplexStatsParser(DemultiplexStatsParser demultiplexStatsParser) {
        this.demultiplexStatsParser = demultiplexStatsParser;
    }

    public void setMetricsRecordWriter(
            MetricsRecordWriter metricsRecordWriter) {
        this.metricsRecordWriter = metricsRecordWriter;
    }
}
