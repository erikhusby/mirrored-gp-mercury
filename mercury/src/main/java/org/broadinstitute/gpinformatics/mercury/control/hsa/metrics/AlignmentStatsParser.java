package org.broadinstitute.gpinformatics.mercury.control.hsa.metrics;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;

import javax.enterprise.context.Dependent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Dependent
public class AlignmentStatsParser {

    private static final Log log = LogFactory.getLog(AlignmentStatsParser.class);

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    public AlignmentDataFiles parseStats(File outputDirectory, String filePrefix, DragenReplayInfo dragenReplayInfo,
                                         MessageCollection messageCollection,
                                         Map<String, String> mapReadGroupToSampleAlias,
                                         Pair<String, String> runNameDatePair) {
        try {
            File mappingMetricsFile = new File(outputDirectory, filePrefix + ".mapping_metrics.csv");
            File mappingMetricsOutputFile = new File(outputDirectory, filePrefix + ".mapping_metrics_mercury.dat");
            File mappingSummaryOutputFile = new File(outputDirectory, filePrefix + ".mapping_summary_mercury.dat");
            if (mappingMetricsFile.exists()) {
                parseMappingMetrics(mappingMetricsFile, mappingSummaryOutputFile, mappingMetricsOutputFile,
                        runNameDatePair.getLeft(), new Date(), dragenReplayInfo, mapReadGroupToSampleAlias, runNameDatePair.getRight());
            } else {
                messageCollection.addError("Failed to find mapping metrics file " + mappingMetricsFile.getPath());
            }

            File vcMetricsFile = new File(outputDirectory, filePrefix + ".vc_metrics.csv");
            File vcMetricsOutputFile = new File(outputDirectory, filePrefix + ".vc_metrics_mercury.dat");
            File vcSummaryOutputFile = new File(outputDirectory, filePrefix + ".vc_summary_mercury.dat");
            if (vcMetricsFile.exists()) {
                parseVcMetrics(vcMetricsFile, vcSummaryOutputFile, vcMetricsOutputFile, runNameDatePair.getLeft(),
                        new Date(), dragenReplayInfo,  runNameDatePair.getRight());
            } else {
                messageCollection.addError("Failed to find vc metrics file " + vcMetricsFile.getPath());
            }

            String alignSummaryLoad = String.format("%s_AlignRun_load.log", filePrefix);
            String alignMetricLoad = String.format("%s_AlignRg_load.log", filePrefix);
            String vcSummaryMetricLoad = String.format("%s_VCRunload.log", filePrefix);
            String vcRGMetricLoad = String.format("%s_VCRGload.log", filePrefix);
            return new AlignmentDataFiles(mappingSummaryOutputFile, mappingMetricsOutputFile, vcSummaryOutputFile, vcMetricsOutputFile,
                    alignSummaryLoad, alignMetricLoad, vcSummaryMetricLoad, vcRGMetricLoad);
        } catch (Exception e) {
            log.error("Error parsing alignment/vc metrics", e);
            messageCollection.addError("Error parsing alignment/vc metrics");
        }

        return null;
    }

    private void parseVcMetrics(File vcMetricsFile, File vcSummaryOutputFile, File vcMetricsOutputFile,
                                String runName, Date runDate,
                                DragenReplayInfo dragenReplayInfo, String analysisName) throws IOException {
        Writer writer = new BufferedWriter(new FileWriter(vcSummaryOutputFile));
        CSVWriter csvWriterSummary = new CSVWriter(writer,
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);

        List<String> vcSummary = new ArrayList<>();
        vcSummary.add(runName);
        vcSummary.add(simpleDateFormat.format(runDate));
        vcSummary.add(analysisName);
        vcSummary.add(dragenReplayInfo.getSystem().getNodename());
        vcSummary.add(dragenReplayInfo.getSystem().getDragenVersion());

        CSVReader csvReader = new CSVReader(new BufferedReader(new FileReader(vcMetricsFile)));
        String[] nextRecord;
        String recordType = null;
        String recordValue = null;
        while ((nextRecord = csvReader.readNext()) != null) {
            recordType = nextRecord[0];
            recordValue = nextRecord[3];

            if (recordType.equals("VARIANT CALLER PREFILTER")) {
                break;
            } else {
                vcSummary.add(recordValue);
            }
        }

        csvWriterSummary.writeNext(vcSummary.toArray(new String[0]));
        csvWriterSummary.close();

        // Metrics
        List<String> vcMetricsPreFilter = new ArrayList<>();
        List<String> vcMetricsPostFilter = new ArrayList<>();
        vcMetricsPreFilter.add(runName);
        vcMetricsPreFilter.add(simpleDateFormat.format(runDate));
        vcMetricsPreFilter.add(analysisName);
        vcMetricsPostFilter.add(runName);
        vcMetricsPostFilter.add(simpleDateFormat.format(runDate));
        vcMetricsPostFilter.add(analysisName);
        vcMetricsPreFilter.add("VARIANT CALLER PREFILTER");
        vcMetricsPostFilter.add("VARIANT CALLER POSTFILTER");

        String sampleAlias = nextRecord[1];
        vcMetricsPreFilter.add(sampleAlias);
        vcMetricsPostFilter.add(sampleAlias);

        Writer metricsWriter = new BufferedWriter(new FileWriter(vcMetricsOutputFile));
        CSVWriter csvWriterMetrics = new CSVWriter(metricsWriter,
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);

        while ((nextRecord = csvReader.readNext()) != null) {
            recordType = nextRecord[0];
            recordValue = nextRecord[3];

            if (recordType.equals("VARIANT CALLER PREFILTER")) {
                vcMetricsPreFilter.add(recordValue);
            } else if (recordType.equals("VARIANT CALLER POSTFILTER")){
                vcMetricsPostFilter.add(recordValue);
            }
        }

        csvWriterMetrics.writeNext(vcMetricsPreFilter.toArray(new String[0]));
        csvWriterMetrics.writeNext(vcMetricsPostFilter.toArray(new String[0]));
        csvWriterMetrics.close();
    }

    private void parseMappingMetrics(File vcMetricsFile, File mappingSummaryOutputFile, File mappingMetricsOutputFile,
                                     String runName, Date runDate, DragenReplayInfo dragenReplayInfo,
                                     Map<String, String> mapReadGroupToSampleAlias, String analysisName) throws IOException {
        Writer summaryWriter = new BufferedWriter(new FileWriter(mappingSummaryOutputFile));
        CSVWriter csvWriterSummary = new CSVWriter(summaryWriter,
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);

        List<String> alignmentSummary = new ArrayList<>();
        List<String> alignmentPerReadGroup = new ArrayList<>();
        alignmentSummary.add(runName);
        alignmentSummary.add(simpleDateFormat.format(runDate));
        alignmentSummary.add(analysisName);
        alignmentSummary.add(dragenReplayInfo.getSystem().getNodename());
        alignmentSummary.add(dragenReplayInfo.getSystem().getDragenVersion());

        CSVReader csvReader = new CSVReader(new BufferedReader(new FileReader(vcMetricsFile)));
        String[] nextRecord;
        String recordType = null;
        String recordField = null;
        String recordValue = null;

        // Want to share some metrics between summary and read group since most alignments are single sample
        Map<String, String> mapFieldToVal = new HashMap<>();
        while ((nextRecord = csvReader.readNext()) != null) {
            recordType = nextRecord[0];
            recordField = nextRecord[2];
            recordValue = nextRecord[3];

            if (!recordType.equals("MAPPING/ALIGNING SUMMARY")) {
                break;
            } else {
                alignmentSummary.add(recordValue);
                mapFieldToVal.put(recordField, recordValue);
            }
        }

        csvWriterSummary.writeNext(alignmentSummary.toArray(new String[0]));
        csvWriterSummary.close();

        Writer metricsWriter = new BufferedWriter(new FileWriter(mappingMetricsOutputFile));
        CSVWriter csvWriterMetrics = new CSVWriter(metricsWriter,
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);

        String readGroup = nextRecord[1];

        alignmentPerReadGroup.add(runName);
        alignmentPerReadGroup.add(simpleDateFormat.format(runDate));
        alignmentPerReadGroup.add(analysisName);
        alignmentPerReadGroup.add(readGroup);
        alignmentPerReadGroup.add(mapReadGroupToSampleAlias.get(readGroup));
        alignmentPerReadGroup.add(recordValue); // total reads

        while ((nextRecord = csvReader.readNext()) != null) {
            recordValue = nextRecord[3];
            alignmentPerReadGroup.add(recordValue);
        }

        for (SharedMetrics sharedMetrics: SharedMetrics.values()) {
            alignmentPerReadGroup.add(mapFieldToVal.get(sharedMetrics.getRecordType()));
        }

        csvWriterMetrics.writeNext(alignmentPerReadGroup.toArray(new String[0]));
        csvWriterMetrics.close();
    }

    public class AlignmentDataFiles {

        private final File mappingSummaryOutputFile;
        private final File mappingMetricsOutputFile;
        private final File vcSummaryOutputFile;
        private final File vcMetricsOutputFile;
        private final String alignSummaryLoad;
        private final String alignMetricLoad;
        private final String vcSummaryMetricLoad;
        private final String vcRGMetricLoad;


        public AlignmentDataFiles(File mappingSummaryOutputFile, File mappingMetricsOutputFile,
                                  File vcSummaryOutputFile,
                                  File vcMetricsOutputFile, String alignSummaryLoad, String alignMetricLoad,
                                  String vcSummaryMetricLoad, String vcRGMetricLoad) {

            this.mappingSummaryOutputFile = mappingSummaryOutputFile;
            this.mappingMetricsOutputFile = mappingMetricsOutputFile;
            this.vcSummaryOutputFile = vcSummaryOutputFile;
            this.vcMetricsOutputFile = vcMetricsOutputFile;
            this.alignSummaryLoad = alignSummaryLoad;
            this.alignMetricLoad = alignMetricLoad;
            this.vcSummaryMetricLoad = vcSummaryMetricLoad;
            this.vcRGMetricLoad = vcRGMetricLoad;
        }

        public File getMappingSummaryOutputFile() {
            return mappingSummaryOutputFile;
        }

        public File getMappingMetricsOutputFile() {
            return mappingMetricsOutputFile;
        }

        public File getVcSummaryOutputFile() {
            return vcSummaryOutputFile;
        }

        public File getVcMetricsOutputFile() {
            return vcMetricsOutputFile;
        }

        public String getAlignSummaryLoad() {
            return alignSummaryLoad;
        }

        public String getAlignMetricLoad() {
            return alignMetricLoad;
        }

        public String getVcSummaryMetricLoad() {
            return vcSummaryMetricLoad;
        }

        public String getVcRGMetricLoad() {
            return vcRGMetricLoad;
        }
    }

    public enum SharedMetrics {
        CONTAMINATION("Estimated sample contamination"),
        PREDICTED_SEX_CHROMOSOME("Predicted sex chromosome ploidy"),
        PCT_COV_100_INF("PCT of genome with coverage [100x:inf)"),
        PCT_COV_50_100("PCT of genome with coverage [50x:100x)"),
        PCT_COV_20_50("PCT of genome with coverage [20x:50x)"),
        PCT_COV_10_20("PCT of genome with coverage [10x:20x)"),
        PCT_COV_3_10("PCT of genome with coverage [ 3x:10x)"),
        PCT_COV_0_3("PCT of genome with coverage [ 0x: 3x)"),
        ;

        private final String recordType;

        private static Map<String, SharedMetrics> mapKeyToMetric = new HashMap<>();

        SharedMetrics(String recordType) {

            this.recordType = recordType;
        }

        static {
            for (SharedMetrics sharedMetrics: SharedMetrics.values()) {
                mapKeyToMetric.put(sharedMetrics.getRecordType(), sharedMetrics);
            }
        }

        public String getRecordType() {
            return recordType;
        }

        public static boolean containsKey(String key) {
            return mapKeyToMetric.containsKey(key);
        }
    }
}
