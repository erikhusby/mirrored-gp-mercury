package org.broadinstitute.gpinformatics.mercury.control.hsa.metrics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;

import javax.enterprise.context.Dependent;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@Dependent
public class BclMetricsParser {

    private static final Log log = LogFactory.getLog(BclMetricsParser.class);

    private static final DecimalFormat decimalFormatter = new DecimalFormat("#.##");

    public List<DemultiplexStats> parseStats(InputStream inputStream, MessageCollection messageCollection) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE);
            StatsRun statsRun = objectMapper.readValue(inputStream, StatsRun.class);
            List<DemultiplexStats> demultiplexStatsList = new ArrayList<>();
            for (ConversionResults conversionResults: statsRun.getConversionResults()) {
                for (DemuxResults demuxResults: conversionResults.getDemuxResults()) {
                    DemultiplexStats demultiplexStats = new DemultiplexStats();
                    demultiplexStats.setLane(conversionResults.getLaneNumber());
                    parseDemuxResults(demultiplexStats, demuxResults);
                    demultiplexStatsList.add(demultiplexStats);
                }

                DemultiplexStats unknownStats = new DemultiplexStats();
                unknownStats.setLane(conversionResults.getLaneNumber());
                demultiplexStatsList.add(unknownStats);
                parseDemuxResults(unknownStats, conversionResults.getUndetermined());
            }
            return demultiplexStatsList;
        } catch (Exception e) {
            messageCollection.addError("Failed to parse json file into stats: " + e.getMessage());
            log.error("Failed to parse json file into stats", e);
        }
        return null;
    }

    private void parseDemuxResults(DemultiplexStats demultiplexStats, DemuxResults demuxResults) {
        String sampleName = demuxResults.getSampleName();
        if (sampleName == null) {
            sampleName = "Undetermined";
        }
        demultiplexStats.setSampleID(sampleName);
        demultiplexStats.setNumberOfReads(demuxResults.getNumberReads());
        if (demuxResults.getIndexMetrics() != null) {
            IndexMetrics indexMetrics = demuxResults.getIndexMetrics().iterator().next();
            demultiplexStats.setIndex(indexMetrics.getIndexSequence().replace("+", "-"));
            demultiplexStats.setNumberOfPerfectIndexReads(indexMetrics.getMismatchCounts().getZero());
            demultiplexStats.setNumberOfOneMismatchIndexreads(indexMetrics.getMismatchCounts().getOne());
        }
        Long numQ30Pf = demuxResults.getReadMetrics().stream()
                .map(ReadMetrics::getYieldQ30)
                .reduce(0L, Long::sum);
        demultiplexStats.setNumberOfQ30BasesPassingFilter(new BigDecimal(numQ30Pf));

        Long qScoreSum = demuxResults.getReadMetrics().stream()
                .map(ReadMetrics::getQualityScoreSum)
                .reduce(0L, Long::sum);

        Long yield = demuxResults.getReadMetrics().stream()
                .map(ReadMetrics::getYield)
                .reduce(0L, Long::sum);

        double meanQScore = (double) qScoreSum / yield;
        demultiplexStats.setMeanQualityScorePassingFilter(decimalFormatter.format(meanQScore));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StatsRun {
        private String flowcell;
        private float runNumber;
        private String runId;
        private List<ConversionResults> conversionResults;

        public StatsRun() {
        }

        public String getFlowcell() {
            return flowcell;
        }

        public void setFlowcell(String flowcell) {
            this.flowcell = flowcell;
        }

        public float getRunNumber() {
            return runNumber;
        }

        public void setRunNumber(float runNumber) {
            this.runNumber = runNumber;
        }

        public String getRunId() {
            return runId;
        }

        public void setRunId(String runId) {
            this.runId = runId;
        }

        public List<ConversionResults> getConversionResults() {
            return conversionResults;
        }

        public void setConversionResults(
                List<ConversionResults> conversionResults) {
            this.conversionResults = conversionResults;
        }
    }

    public static class ConversionResults {
        private int laneNumber;
        private long totalClustersRaw;
        private long totalClustersPF;
        private long yield;
        private List<DemuxResults> demuxResults;
        private DemuxResults undetermined;

        public ConversionResults() {
        }

        public int getLaneNumber() {
            return laneNumber;
        }

        public void setLaneNumber(int laneNumber) {
            this.laneNumber = laneNumber;
        }

        public long getTotalClustersRaw() {
            return totalClustersRaw;
        }

        public void setTotalClustersRaw(long totalClustersRaw) {
            this.totalClustersRaw = totalClustersRaw;
        }

        public long getTotalClustersPF() {
            return totalClustersPF;
        }

        public void setTotalClustersPF(long totalClustersPF) {
            this.totalClustersPF = totalClustersPF;
        }

        public long getYield() {
            return yield;
        }

        public void setYield(long yield) {
            this.yield = yield;
        }

        public List<DemuxResults> getDemuxResults() {
            return demuxResults;
        }

        public void setDemuxResults(List<DemuxResults> demuxResults) {
            this.demuxResults = demuxResults;
        }

        public DemuxResults getUndetermined() {
            return undetermined;
        }

        public void setUndetermined(
                DemuxResults undetermined) {
            this.undetermined = undetermined;
        }
    }

    public static class DemuxResults {
        private String SampleId;
        private String sampleName;
        private List<IndexMetrics> indexMetrics;
        private List<ReadMetrics> readMetrics;
        private long numberReads;
        private long yield;

        public DemuxResults() {
        }

        public String getSampleId() {
            return SampleId;
        }

        public void setSampleId(String sampleId) {
            SampleId = sampleId;
        }

        public String getSampleName() {
            return sampleName;
        }

        public void setSampleName(String sampleName) {
            this.sampleName = sampleName;
        }

        public List<IndexMetrics> getIndexMetrics() {
            return indexMetrics;
        }

        public void setIndexMetrics(
                List<IndexMetrics> indexMetrics) {
            this.indexMetrics = indexMetrics;
        }

        public List<ReadMetrics> getReadMetrics() {
            return readMetrics;
        }

        public void setReadMetrics(
                List<ReadMetrics> readMetrics) {
            this.readMetrics = readMetrics;
        }

        public long getNumberReads() {
            return numberReads;
        }

        public void setNumberReads(long numberReads) {
            this.numberReads = numberReads;
        }

        public long getYield() {
            return yield;
        }

        public void setYield(long yield) {
            this.yield = yield;
        }
    }

    public static class IndexMetrics {
        private String indexSequence;
        private MismatchCounts mismatchCounts;

        public IndexMetrics() {
        }

        public String getIndexSequence() {
            return indexSequence;
        }

        public void setIndexSequence(String indexSequence) {
            this.indexSequence = indexSequence;
        }

        public MismatchCounts getMismatchCounts() {
            return mismatchCounts;
        }

        public void setMismatchCounts(
                MismatchCounts mismatchCounts) {
            this.mismatchCounts = mismatchCounts;
        }
    }

    public static class MismatchCounts {

        @JsonProperty("0")
        private long zero;

        @JsonProperty("1")
        private long one;

        public MismatchCounts() {
        }

        public long getZero() {
            return zero;
        }

        public void setZero(long zero) {
            this.zero = zero;
        }

        public long getOne() {
            return one;
        }

        public void setOne(long one) {
            this.one = one;
        }
    }

    public static class ReadMetrics {
        private int readNumber;
        private long yield;
        private long yieldQ30;
        private long qualityScoreSum;
        private long trimmedBases;

        public ReadMetrics() {
        }

        public int getReadNumber() {
            return readNumber;
        }

        public void setReadNumber(int readNumber) {
            this.readNumber = readNumber;
        }

        public long getYield() {
            return yield;
        }

        public void setYield(long yield) {
            this.yield = yield;
        }

        public long getYieldQ30() {
            return yieldQ30;
        }

        public void setYieldQ30(long yieldQ30) {
            this.yieldQ30 = yieldQ30;
        }

        public long getQualityScoreSum() {
            return qualityScoreSum;
        }

        public void setQualityScoreSum(long qualityScoreSum) {
            this.qualityScoreSum = qualityScoreSum;
        }

        public long getTrimmedBases() {
            return trimmedBases;
        }

        public void setTrimmedBases(long trimmedBases) {
            this.trimmedBases = trimmedBases;
        }
    }
}
