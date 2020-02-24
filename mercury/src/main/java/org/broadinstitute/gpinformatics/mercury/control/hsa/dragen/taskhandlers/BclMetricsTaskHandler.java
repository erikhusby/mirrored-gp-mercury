package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.DemultiplexLaneMetric;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.DemultiplexSampleMetric;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.Bcl2FastqTaskBuilder;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.BclDemultiplexMetricsTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.BclDemultiplexTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.BclMetricsParser;
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.DemultiplexStats;
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.DragenReplayInfo;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.JobInfo;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.DemultiplexState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;

import javax.enterprise.context.Dependent;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;

@Dependent
public class BclMetricsTaskHandler extends BaseDemultiplexMetricsHandler<BclDemultiplexMetricsTask> {

    private static final Log log = LogFactory.getLog(BclMetricsTaskHandler.class);

    @Override
    public void handleTask(BclDemultiplexMetricsTask task, SchedulerContext schedulerContext) {
        State state = task.getState();
        if (!OrmUtil.proxySafeIsInstance(state, DemultiplexState.class)) {
            throw new RuntimeException("Expect only a demultiplex state for a BclMetricsTask.");
        }
        try {
            DemultiplexState demultiplexState = OrmUtil.proxySafeCast(state, DemultiplexState.class);
            BclDemultiplexTask demuxTask = demultiplexState.getTasksOfType(BclDemultiplexTask.class).iterator().next();

            DragenReplayInfo dragenReplayInfo = new DragenReplayInfo();
            DragenReplayInfo.System system = new DragenReplayInfo.System();
            system.setDragenVersion(Bcl2FastqTaskBuilder.CURRENT_VERSION);
            JobInfo jobInfo = schedulerContext.getInstance().fetchJobInfo(demuxTask.getProcessId());
            if (jobInfo != null) {
                system.setNodename(jobInfo.getNodeList());
            }
            dragenReplayInfo.setSystem(system);

            File runDirectory = demuxTask.getRunDirectory();
            File statsFile = getStatsFile(demuxTask.getOutputDirectory());
            IlluminaSequencingRun run = findRunFromDirectory(runDirectory, demultiplexState.getSequencingRunChambers());
            if (run == null) {
                throw new RuntimeException(
                        "Failed to find run in demux state " + runDirectory.getPath() + " " + demultiplexState
                                .getStateId());
            }

            MessageCollection messageCollection = new MessageCollection();
            BclMetricsParser metricsParser = new BclMetricsParser();
            List<DemultiplexStats> demultiplexStats =
                    metricsParser.parseStats(new FileInputStream(statsFile), messageCollection);
            Pair<List<DemultiplexSampleMetric>, List<DemultiplexLaneMetric>> pair =
                    createSequencingRunMetricDaoFree(demultiplexState, run, messageCollection, dragenReplayInfo,
                            demultiplexStats);

            List<DemultiplexSampleMetric> sequencingMetricRun = pair.getLeft();
            List<DemultiplexLaneMetric> laneMetrics = pair.getRight();
            uploadResults(task, run, statsFile.getParentFile(), sequencingMetricRun, laneMetrics);
        } catch (Exception e) {
            log.error("Failed to parse metrics for task " + task.getTaskId(), e);
            task.setStatus(Status.FAILED);
        }
    }



    public File getStatsFile(File analysisDir) {
        File statsFolder = new File(analysisDir, "Stats");
        return new File(statsFolder, "Stats.json");
    }
}
