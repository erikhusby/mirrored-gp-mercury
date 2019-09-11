package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.deployment.DragenConfig;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DemultiplexMetricsTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DragenFolderUtil;
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.DemultiplexStats;
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.DemultiplexStatsParser;
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.DragenReplayInfo;
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.MetricsRecordWriter;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.ShellUtils;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.DemultiplexState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.SequencingDemultiplexMetric;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.zeroturnaround.exec.ProcessResult;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

@Dependent
public class DemultiplexMetricsTaskHandler extends AbstractTaskHandler {

    private static final Log log = LogFactory.getLog(DemultiplexMetricsTaskHandler.class);

    @Inject
    private DemultiplexStatsParser demultiplexStatsParser;

    @Inject
    private DragenConfig dragenConfig;

    @Inject
    private MetricsRecordWriter metricsRecordWriter;

    @Inject
    private ShellUtils shellUtils;

    // For testing
    private List<DemultiplexStats> demultiplexStats;

    private DragenReplayInfo dragenReplayInfo;

    @Override
    public void handleTask(Task task, SchedulerContext schedulerContext) {
        DemultiplexMetricsTask demultiplexTask = OrmUtil.proxySafeCast(task, DemultiplexMetricsTask.class);

        State state = demultiplexTask.getState();
        if (!OrmUtil.proxySafeIsInstance(state, DemultiplexState.class)) {
            throw new RuntimeException("Expect only a demultiplex state for a demultiplex metrics task.");
        }
        DemultiplexState demultiplexState = OrmUtil.proxySafeCast(state, DemultiplexState.class);

        DragenFolderUtil dragenFolderUtil = new DragenFolderUtil(dragenConfig, demultiplexState.getRun(), demultiplexState.getStateName());
        File demultiplexMetricsFile = dragenFolderUtil.getDemultiplexStatsFile();
        if (!demultiplexMetricsFile.exists()) {
            String errMsg = "Demultiplex Metrics file doesn't exist: " + demultiplexMetricsFile.getPath();
            log.info(errMsg);
            task.setErrorMessage(errMsg);
            task.setStatus(Status.QUEUED);
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

        List<SequencingDemultiplexMetric> sequencingMetricRun =
                createSequencingRunMetricDaoFree(demultiplexState, messageCollection, dragenReplayInfo, demultiplexStats);

        if (!messageCollection.hasErrors()) {
            try {
                ColumnPositionMappingStrategy<SequencingDemultiplexMetric> mappingStrategy =
                        new ColumnPositionMappingStrategy<>();
                mappingStrategy.setType(SequencingDemultiplexMetric.class);
                File outputRecord = new File(dragenFolderUtil.getReportsFolder(), "demultiplex_metrics.dat");
                metricsRecordWriter.writeBeanRecord(sequencingMetricRun, outputRecord, mappingStrategy);

                String ldruid = "mercurydw/seq_dev3@\"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=seqdev.broad.mit.edu)(PORT=1521)))(CONNECT_DATA=(SERVER=DEDICATED)(SID=seqdev3)))\"";

                List<String> cmds = Arrays.asList("sqlldr",
                        "control=/seq/lims/datawh/dev/dragen/demultiplex_metric.ctl",
                        "log=load.log",
                        "bad=load.bad",
                        String.format("data=%s", outputRecord.getPath()),
                        "discard=load.dsc",
                        "direct=false",
                        String.format("userId=%s", ldruid));
                ProcessResult processResult = shellUtils.runSyncProcess(cmds);
                if (processResult.getExitValue() == 0) { // TODO Exit value?
                    task.setStatus(Status.COMPLETE);
                    if (processResult.hasOutput()) {
                        log.info(processResult.getOutput().getString());
                    }
                } else {
                    task.setStatus(Status.FAILED);
                }
            } catch (CsvDataTypeMismatchException e) {
                String errMsg = "Data type mismatch";
                log.error(errMsg, e);
                task.setErrorMessage(errMsg);
                task.setStatus(Status.FAILED);
            } catch (CsvRequiredFieldEmptyException e) {
                String errMsg = "Missing required field";
                log.error(errMsg, e);
                task.setErrorMessage(errMsg);
                task.setStatus(Status.FAILED);
            } catch (IOException e) {
                String errMsg = "Error writing to file";
                task.setErrorMessage(errMsg);
                log.error(errMsg, e);
                task.setStatus(Status.FAILED);
            } catch (InterruptedException e) {
                String errMsg = "sqlloader process thread interrupted";
                task.setErrorMessage(errMsg);
                log.error(errMsg, e);
                task.setStatus(Status.FAILED);
            } catch (TimeoutException e) {
                String errMsg = "sqlloader process thread timed out.";
                task.setErrorMessage(errMsg);
                log.error(errMsg, e);
                task.setStatus(Status.FAILED);
            }
        } else {
            log.error("Errors processing demultiplex stats" +
                      StringUtils.join(messageCollection.getErrors(), ","));
            task.setStatus(Status.FAILED);
        }
    }

    @DaoFree
    public List<SequencingDemultiplexMetric> createSequencingRunMetricDaoFree(DemultiplexState demultiplexState,
                                                                              MessageCollection messageCollection,
                                                                              DragenReplayInfo dragenReplayInfo,
                                                                              List<DemultiplexStats> demultiplexStats) {
        Map<String, Set<Integer>> mapSampleToLanes = new HashMap<>();

        RunCartridge flowcell = demultiplexState.getRun().getSampleCartridge();
        int laneNum = 0;
        for (VesselPosition vesselPosition: flowcell.getVesselGeometry().getVesselPositions()) {
            ++laneNum;
            for (SampleInstanceV2 sampleInstanceV2 : flowcell.getContainerRole()
                    .getSampleInstancesAtPositionV2(vesselPosition)) {
                ProductOrderSample productOrderSample = sampleInstanceV2.getSingleProductOrderSample();
                MercurySample mercurySample = null;
                if (productOrderSample != null) {
                    mercurySample = productOrderSample.getMercurySample();
                } else {
                    // Controls won't have a ProductOrderSample, so use root sample ID.
                    mercurySample = sampleInstanceV2.getRootOrEarliestMercurySample();
                }
                if (!mapSampleToLanes.containsKey(mercurySample.getSampleKey())) {
                    mapSampleToLanes.put(mercurySample.getSampleKey(),  new HashSet<>());
                }
                mapSampleToLanes.get(mercurySample.getSampleKey()).add(laneNum);
            }
        }

        // Verify that the demultiplex stat is in seq run
        List<SequencingDemultiplexMetric> sequencingMetrics = new ArrayList<>();
        for (DemultiplexStats stat: demultiplexStats) {
            if (stat.getSampleID().equals("Undetermined")) {
                // TODO JW handle undetermines
                continue;
            }
            if (!mapSampleToLanes.containsKey(stat.getSampleID())) {
                messageCollection.addError("Unexpected Sample ID in Demultiplex Stats file " + stat.getSampleID());
            } else if (!mapSampleToLanes.get(stat.getSampleID()).contains(stat.getLane())) {
                messageCollection.addError("Unexpected lane in Demultiplex Stats file " + stat.getLane());
            } else {
                int lane = stat.getLane();
                String sampleAlias = stat.getSampleID();

                SequencingDemultiplexMetric sequencingMetric = new SequencingDemultiplexMetric(lane, sampleAlias);
                sequencingMetric.setRunName(demultiplexState.getRun().getRunName());
                sequencingMetric.setRunDate(demultiplexState.getRun().getRunDate());
                sequencingMetric.setFlowcell(demultiplexState.getRun().getSampleCartridge().getLabel());
                sequencingMetric.setDragenVersion(dragenReplayInfo.getSystem().getDragenVersion());
                sequencingMetric.setAnalysisNode(dragenReplayInfo.getSystem().getNodename());
                sequencingMetric.setAnalysisName(demultiplexState.getStateName());
                if (stat.getMeanQualityScorePassingFilter() != null &&
                    !stat.getMeanQualityScorePassingFilter().toLowerCase().contains("nan")) {
                    sequencingMetric.setMeanQualityScorePF(stat.getMeanQualityScorePassingFilter());
                } else {
                    sequencingMetric.setMeanQualityScorePF("");
                }
                sequencingMetric.setNumberOfQ30BasesPF(stat.getNumberOfQ30BasesPassingFilter());
                sequencingMetric.setNumberOfReads(stat.getNumberOfReads());
                sequencingMetric.setNumberOfPerfectReads(stat.getNumberOfPerfectIndexReads());
                sequencingMetric.setNumberOfOneMismatchIndexReads(stat.getNumberOfOneMismatchIndexreads());
                sequencingMetrics.add(sequencingMetric);
            }
        }

        return sequencingMetrics;
    }

    public List<DemultiplexStats> getDemultiplexStats() {
        return demultiplexStats;
    }

    public DragenReplayInfo getDragenReplayInfo() {
        return dragenReplayInfo;
    }

    // For Testing
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

    public void setShellUtils(ShellUtils shellUtils) {
        this.shellUtils = shellUtils;
    }
}
