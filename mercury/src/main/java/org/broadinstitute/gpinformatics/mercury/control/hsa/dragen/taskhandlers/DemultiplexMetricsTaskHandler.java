package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.DemultiplexLaneMetric;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.DemultiplexSampleMetric;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import static java.util.stream.Collectors.groupingBy;

@Dependent
public class DemultiplexMetricsTaskHandler extends AbstractMetricsTaskHandler {

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

        Pair<List<DemultiplexSampleMetric>, List<DemultiplexLaneMetric>> pair =
                createSequencingRunMetricDaoFree(demultiplexState, messageCollection, dragenReplayInfo, demultiplexStats);
        List<DemultiplexSampleMetric> sequencingMetricRun = pair.getLeft();
        List<DemultiplexLaneMetric> laneMetrics = pair.getRight();

        if (!messageCollection.hasErrors()) {
            try {
                // TODO Cleanup and move tasks to their own
                ColumnPositionMappingStrategy<DemultiplexSampleMetric> mappingStrategy =
                        new ColumnPositionMappingStrategy<>();
                mappingStrategy.setType(DemultiplexSampleMetric.class);
                File outputRecord = new File(dragenFolderUtil.getReportsFolder(), "demultiplex_metrics.dat");
                metricsRecordWriter.writeBeanRecord(sequencingMetricRun, outputRecord, mappingStrategy);

                List<ProcessResult> processResults = new ArrayList<>();
                processResults.add(uploadMetric(
                        "/seq/lims/datawh/dev/dragen/demultiplex_metric.ctl", outputRecord));

                ColumnPositionMappingStrategy<DemultiplexLaneMetric> laneStrategy =
                        new ColumnPositionMappingStrategy<>();
                laneStrategy.setType(DemultiplexLaneMetric.class);
                outputRecord = new File(dragenFolderUtil.getReportsFolder(), "demultiplex_lane_metrics.dat");
                metricsRecordWriter.writeBeanRecord(laneMetrics, outputRecord, laneStrategy);

                processResults.add(uploadMetric(
                        "/seq/lims/datawh/dev/dragen/demultiplex_lane_metric.ctl", outputRecord));

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
    public Pair<List<DemultiplexSampleMetric>, List<DemultiplexLaneMetric>> createSequencingRunMetricDaoFree(DemultiplexState demultiplexState,
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
        List<DemultiplexSampleMetric> sequencingMetrics = new ArrayList<>();

        Map<Integer, Long> mapLaneToUndeterminedReads = new HashMap<>();
        Map<Integer, DemultiplexLaneMetric> mapLaneToLaneMetric = new HashMap<>();
        Map<Integer, Long> mapLaneToReads = new HashMap<>();
        for (DemultiplexStats stat: demultiplexStats) {
            int lane = stat.getLane();
            String sampleId = stat.getSampleID().split("_")[0];
            if (sampleId.equals("Undetermined")) {
                mapLaneToUndeterminedReads.put(lane,  stat.getNumberOfReads());
                continue;
            }
            if (!mapSampleToLanes.containsKey(sampleId)) {
                messageCollection.addError("Unexpected Sample ID in Demultiplex Stats file " + sampleId);
            } else if (!mapSampleToLanes.get(sampleId).contains(stat.getLane())) {
                messageCollection.addError("Unexpected lane in Demultiplex Stats file " + stat.getLane());
            } else {
                String sampleAlias = sampleId;

                DemultiplexSampleMetric sequencingMetric = new DemultiplexSampleMetric(lane, sampleAlias);
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

                if (!mapLaneToReads.containsKey(lane)) {
                    mapLaneToReads.put(lane, 0L);
                    DemultiplexLaneMetric laneMetric = new DemultiplexLaneMetric();
                    laneMetric.setRunName(demultiplexState.getRun().getRunName());
                    laneMetric.setRunDate(demultiplexState.getRun().getRunDate());
                    laneMetric.setFlowcell(demultiplexState.getRun().getSampleCartridge().getLabel());
                    laneMetric.setDragenVersion(dragenReplayInfo.getSystem().getDragenVersion());
                    laneMetric.setAnalysisNode(dragenReplayInfo.getSystem().getNodename());
                    laneMetric.setAnalysisName(demultiplexState.getStateName());
                    laneMetric.setLane(lane);
                    mapLaneToLaneMetric.put(lane, laneMetric);
                }
                mapLaneToReads.put(lane, mapLaneToReads.get(lane) + stat.getNumberOfReads() + stat.getNumberOfOneMismatchIndexreads());
            }
        }

        for (Map.Entry<Integer, Long> undeterminedEntry: mapLaneToUndeterminedReads.entrySet()) {
            int lane = undeterminedEntry.getKey();
            Long mappedReads = mapLaneToReads.get(lane);
            Long undeterminedReads = mapLaneToUndeterminedReads.get(lane);
            double orphanRate = ((double)mappedReads) / undeterminedReads;
            mapLaneToLaneMetric.get(lane).setOrphanRate(new BigDecimal(orphanRate));
        }

        return Pair.of(sequencingMetrics, new ArrayList<>(mapLaneToLaneMetric.values()));
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
