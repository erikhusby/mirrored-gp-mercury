package org.broadinstitute.gpinformatics.mercury.control.hsa.metrics;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
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
import java.util.List;
import java.util.Map;

@Dependent
public class AlignmentStatsParser {

    private static final Log log = LogFactory.getLog(AlignmentStatsParser.class);

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    public AlignmentDataFiles parseStats(File outputDirectory, String filePrefix, DragenReplayInfo dragenReplayInfo,
                                           MessageCollection messageCollection, Map<String, String> mapReadGroupToSampleAlias) {
        try {
            File mappingMetricsFile = new File(outputDirectory, filePrefix + ".mapping_metrics.csv");
            File mappingMetricsOutputFile = new File(outputDirectory, filePrefix + ".mapping_metrics_mercury.dat");
            File mappingSummaryOutputFile = new File(outputDirectory, filePrefix + ".mapping_summary_mercury.dat");
            if (mappingMetricsFile.exists()) {
                parseMappingMetrics(mappingMetricsFile, mappingSummaryOutputFile, mappingMetricsOutputFile,
                        "runanmetodo", new Date(),
                        dragenReplayInfo, mapReadGroupToSampleAlias);
            } else {
                messageCollection.addError("Failed to find mapping metrics file " + mappingMetricsFile.getPath());
            }

            File vcMetricsFile = new File(outputDirectory, filePrefix + ".vc_metrics.csv");
            File vcMetricsOutputFile = new File(outputDirectory, filePrefix + ".vc_metrics_mercury.dat");
            File vcSummaryOutputFile = new File(outputDirectory, filePrefix + ".vc_summary_mercury.dat");
            if (vcMetricsFile.exists()) {
                parseVcMetrics(vcMetricsFile, vcSummaryOutputFile, vcMetricsOutputFile, "runanmetodo", new Date(), dragenReplayInfo);
            } else {
                messageCollection.addError("Failed to find vc metrics file " + vcMetricsFile.getPath());
            }

            return new AlignmentDataFiles(mappingSummaryOutputFile, mappingMetricsOutputFile, vcSummaryOutputFile, vcMetricsOutputFile);
        } catch (Exception e) {
            log.error("Error parsing alignment/vc metrics", e);
            messageCollection.addError("Error parsing alignment/vc metrics");
        }

        return null;
    }

    private void parseVcMetrics(File vcMetricsFile, File vcSummaryOutputFile, File vcMetricsOutputFile,
                                String runName, Date runDate,
                                DragenReplayInfo dragenReplayInfo) throws IOException {
        Writer writer = new BufferedWriter(new FileWriter(vcSummaryOutputFile));
        CSVWriter csvWriterSummary = new CSVWriter(writer,
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);

        List<String> vcSummary = new ArrayList<>();
        vcSummary.add(runName);
        vcSummary.add(simpleDateFormat.format(runDate));
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
        vcMetricsPostFilter.add(runName);
        vcMetricsPostFilter.add(simpleDateFormat.format(runDate));
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

    private void parseMappingMetrics(File vcMetricsFile, File mappingSummaryOutputFile, File mappingMetricsOutputFile, String runName,
                                     Date runDate, DragenReplayInfo dragenReplayInfo, Map<String, String> mapReadGroupToSampleAlias) throws IOException {
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
        alignmentSummary.add(dragenReplayInfo.getSystem().getNodename());
        alignmentSummary.add(dragenReplayInfo.getSystem().getDragenVersion());

        CSVReader csvReader = new CSVReader(new BufferedReader(new FileReader(vcMetricsFile)));
        String[] nextRecord;
        String recordType = null;
        String recordValue = null;
        while ((nextRecord = csvReader.readNext()) != null) {
            recordType = nextRecord[0];
            recordValue = nextRecord[3];

            if (!recordType.equals("MAPPING/ALIGNING SUMMARY")) {
                break;
            } else {
                alignmentSummary.add(recordValue);
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
        alignmentPerReadGroup.add(readGroup);
        alignmentPerReadGroup.add(mapReadGroupToSampleAlias.get(readGroup));
        alignmentPerReadGroup.add(recordValue); // total reads

        while ((nextRecord = csvReader.readNext()) != null) {
            recordValue = nextRecord[3];
            alignmentPerReadGroup.add(recordValue);
        }

        csvWriterMetrics.writeNext(alignmentPerReadGroup.toArray(new String[0]));
        csvWriterMetrics.close();
    }

    public class AlignmentDataFiles {

        private final File mappingSummaryOutputFile;
        private final File mappingMetricsOutputFile;
        private final File vcSummaryOutputFile;
        private final File vcMetricsOutputFile;

        public AlignmentDataFiles(File mappingSummaryOutputFile, File mappingMetricsOutputFile,
                                  File vcSummaryOutputFile,
                                  File vcMetricsOutputFile) {

            this.mappingSummaryOutputFile = mappingSummaryOutputFile;
            this.mappingMetricsOutputFile = mappingMetricsOutputFile;
            this.vcSummaryOutputFile = vcSummaryOutputFile;
            this.vcMetricsOutputFile = vcMetricsOutputFile;
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
    }
}
