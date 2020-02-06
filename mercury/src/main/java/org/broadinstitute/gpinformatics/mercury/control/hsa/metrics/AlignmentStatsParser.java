package org.broadinstitute.gpinformatics.mercury.control.hsa.metrics;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;

import javax.enterprise.context.Dependent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
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

    public static final String SEQ_COVERAGE_FIELD = "Average sequenced coverage over genome";

    public static final String SUMMARY_TYPE = "MAPPING/ALIGNING SUMMARY";

    public static final String RG_TYPE = "MAPPING/ALIGNING PER RG";

    // TODO Should check aggregation task to see if it has mapping files, vc etc.
    public AlignmentDataFiles parseFolder(String runName, Date runDate, String analysisName,
                                                DragenReplayInfo dragenReplayInfo, File directory, String readGroup,
                                                String filePrefix, boolean sampleContamination) throws IOException {
        File mappingMetricsFile = new File(directory, filePrefix + ".mapping_metrics.csv");
        File covMetrics = new File(directory, filePrefix + ".qc-coverage-region-1_coverage_metrics.csv");
        File vcMetrics = new File(directory, filePrefix + ".vc_metrics.csv");
        File predictedSexChromosomePloidy = new File(directory, filePrefix + ".wgs_ploidy.csv");

        Map<RecordType, String> mapSummaryToValue = new HashMap<>();
        Map<String, Map<RecordType, String>> mapRgToValue = new HashMap<>();
        Map<RecordType, String> mapCovToValue = new HashMap<>();
        Map<RecordType, String> mapVcToPrefilterValue = new HashMap<>();
        Map<RecordType, String> mapVcToPostfilterValue = new HashMap<>();
        Map<RecordType, String> mapVcToPrefilterRates = new HashMap<>();
        Map<RecordType, String> mapVcToPostfilterRates = new HashMap<>();

        CSVReader csvReader = new CSVReader(new BufferedReader(new FileReader(mappingMetricsFile)));
        String recordType = null;
        String readGroupRecord = null;
        String recordField = null;
        String recordValue = null;
        String[] nextRecord = null;
        while ((nextRecord = csvReader.readNext()) != null) {
            recordType = nextRecord[0];
            readGroupRecord = nextRecord[1];
            recordField = nextRecord[2];
            recordValue = nextRecord[3];

            if (recordType.equals(SUMMARY_TYPE) && SummaryFields.getRecordFromField(recordField) != null) {
                mapSummaryToValue.put(SummaryFields.getRecordFromField(recordField), recordValue);
            } else if (recordType.equals(RG_TYPE) && RgFields.getRecordFromField(recordField) != null) {
                if (!mapRgToValue.containsKey(readGroupRecord)) {
                    mapRgToValue.put(readGroupRecord, new HashMap<>());
                }
                mapRgToValue.get(readGroupRecord).put(RgFields.getRecordFromField(recordField), recordValue);
            }
        }

        String predictedSexChromosome = parsePredictedSexChromosomePloidyFile(predictedSexChromosomePloidy);

        csvReader = new CSVReader(new BufferedReader(new FileReader(covMetrics)));

        // Coverage Metrics
        while ((nextRecord = csvReader.readNext()) != null) {
            recordField = nextRecord[2];
            recordValue = nextRecord[3];

            if (CoverageFields.getRecordFromField(recordField) != null) {
                mapCovToValue.put(CoverageFields.getRecordFromField(recordField), recordValue);
            }
        }

        // Vc Metrics
        csvReader = new CSVReader(new BufferedReader(new FileReader(vcMetrics)));

        // Boolean some vc runs skip pre-filter
        boolean isHardSkipFilter = true;
        String numSamples = csvReader.readNext()[3];
        String readsProcessed = csvReader.readNext()[3];
        String childSample = csvReader.readNext()[3];
        while ((nextRecord = csvReader.readNext()) != null) {
            recordType = nextRecord[0];
            recordField = nextRecord[2];
            recordValue = nextRecord[3];

            VcFields vcRecord = VcFields.getRecordFromField(recordField);
            Map<RecordType, String> vcMap = null;
            if (vcRecord != null) {
                boolean isPrefilter = recordType.equals("VARIANT CALLER PREFILTER");
                vcMap = isPrefilter ? mapVcToPrefilterValue : mapVcToPostfilterValue;
                vcMap.put(vcRecord, recordValue);

                if (isPrefilter) {
                    isHardSkipFilter = false;
                }
                if (vcRecord.getIncludeRate()) {
                    String rateRecord = nextRecord[4];
                    Map<RecordType, String> vcRateMap = isPrefilter ? mapVcToPrefilterRates :  mapVcToPostfilterRates;
                    vcRateMap.put(vcRecord, rateRecord);
                }
            }
        }

        // Write outputs for Mapping Metrics
        File mappingSummaryOutputFile = new File(directory, filePrefix + ".mapping_summary_mercury.dat");

        Writer writer = new BufferedWriter(new FileWriter(mappingSummaryOutputFile));
        CSVWriter csvWriterSummary = new CSVWriter(writer,
        CSVWriter.DEFAULT_SEPARATOR,
        CSVWriter.NO_QUOTE_CHARACTER,
        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
        CSVWriter.DEFAULT_LINE_END);

        List<String> summaryResults = new ArrayList<>();
        summaryResults.add(runName);
        summaryResults.add(simpleDateFormat.format(runDate));
        summaryResults.add(readGroup);
        summaryResults.add(filePrefix); // Sample alias
        summaryResults.add(analysisName);
        summaryResults.add(dragenReplayInfo.getSystem().getNodename());
        summaryResults.add(dragenReplayInfo.getSystem().getDragenVersion());
        for (SummaryFields summaryFields: SummaryFields.values()) {
            if (mapSummaryToValue.containsKey(summaryFields)) {
                summaryResults.add(mapSummaryToValue.get(summaryFields));
                // TODO
            } else if (summaryFields == SummaryFields.EST_SAMPLE_CONTAM && !sampleContamination) {
                summaryResults.add("NA");
            }
        }

        for (CoverageFields coverageFields: CoverageFields.values()) {
            if (mapCovToValue.containsKey(coverageFields)) {
                summaryResults.add(mapCovToValue.get(coverageFields));
            }
        }
        summaryResults.add(predictedSexChromosome);
        csvWriterSummary.writeNext(summaryResults.toArray(new String[0]));
        csvWriterSummary.close();

        // Write outputs for each read group
        File mappingRgOutputFile = new File(directory, filePrefix + ".mapping_rg_metrics_mercury.dat");
        writer = new BufferedWriter(new FileWriter(mappingRgOutputFile));
        CSVWriter csvWriterRg = new CSVWriter(writer,
        CSVWriter.DEFAULT_SEPARATOR,
        CSVWriter.NO_QUOTE_CHARACTER,
        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
        CSVWriter.DEFAULT_LINE_END);
        for (Map.Entry<String, Map<RecordType, String>> entry: mapRgToValue.entrySet()) {
            List<String> rgResults = new ArrayList<>();
            rgResults.add(entry.getKey());
            rgResults.add(simpleDateFormat.format(runDate));
            rgResults.add(filePrefix);
            rgResults.add(analysisName);
            rgResults.add(dragenReplayInfo.getSystem().getNodename());
            rgResults.add(dragenReplayInfo.getSystem().getDragenVersion());
            Map<RecordType, String> mapRgRecordToVal = entry.getValue();
            for (RgFields rgField: RgFields.values()) {
                if (mapRgRecordToVal.containsKey(rgField)) {
                    rgResults.add(mapRgRecordToVal.get(rgField));
                }
            }

            csvWriterRg.writeNext(rgResults.toArray(new String[0]));
        }
        csvWriterRg.close();

        // Write Outputs for VC Metrics
        File vcMetricsDat = new File(directory, filePrefix + ".vc_summary_mercury.dat");

        writer = new BufferedWriter(new FileWriter(vcMetricsDat));
        csvWriterSummary = new CSVWriter(writer,
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END);

        List<String> vcPreMetricsList = new ArrayList<>();
        vcPreMetricsList.add(runName);
        vcPreMetricsList.add(simpleDateFormat.format(runDate));
        vcPreMetricsList.add(analysisName);
        vcPreMetricsList.add(dragenReplayInfo.getSystem().getNodename());
        vcPreMetricsList.add(dragenReplayInfo.getSystem().getDragenVersion());
        vcPreMetricsList.add(filePrefix);
        vcPreMetricsList.add("PREFILTER");
        vcPreMetricsList.add(numSamples);
        vcPreMetricsList.add(readsProcessed);
        vcPreMetricsList.add(childSample);

        List<String> vcPostMetricsList = new ArrayList<>();
        vcPostMetricsList.add(runName);
        vcPostMetricsList.add(simpleDateFormat.format(runDate));
        vcPostMetricsList.add(analysisName);
        vcPostMetricsList.add(dragenReplayInfo.getSystem().getNodename());
        vcPostMetricsList.add(dragenReplayInfo.getSystem().getDragenVersion());
        vcPostMetricsList.add(filePrefix);
        vcPostMetricsList.add("POSTFILTER");
        vcPostMetricsList.add(numSamples);
        vcPostMetricsList.add(readsProcessed);
        vcPostMetricsList.add(childSample);

        for (VcFields field: VcFields.values()) {
            if (mapVcToPrefilterValue.containsKey(field)) {
                vcPreMetricsList.add(mapVcToPrefilterValue.get(field));
                if (field.getIncludeRate()) {
                    vcPreMetricsList.add(mapVcToPrefilterRates.get(field));
                }
            } else if (field.isOptional()) {
                vcPreMetricsList.add("NA");
            }

            if (mapVcToPostfilterValue.containsKey(field)) {
                vcPostMetricsList.add(mapVcToPostfilterValue.get(field));
                if (field.getIncludeRate()) {
                    vcPostMetricsList.add(mapVcToPostfilterRates.get(field));
                }
            } else if (field.isOptional()) {
                vcPostMetricsList.add("NA");
            }
        }

        if (!isHardSkipFilter) { // No prefilter metrics, so skip
            csvWriterSummary.writeNext(vcPreMetricsList.toArray(new String[0]));
        }
        csvWriterSummary.writeNext(vcPostMetricsList.toArray(new String[0]));
        csvWriterSummary.close();

        String alignSummaryLoad = String.format("%s_AlignRun_load.log", filePrefix);
        String alignRgLoad = String.format("%s_AlignRg_load.log", filePrefix);
        String vcSummaryMetricLoad = String.format("%s_VCRunload.log", filePrefix);

        AlignmentDataFiles dataFiles = new AlignmentDataFiles(mappingSummaryOutputFile, mappingRgOutputFile,
                vcMetricsDat, null, alignSummaryLoad, alignRgLoad, vcSummaryMetricLoad, null);

        return dataFiles;
    }

    private String parsePredictedSexChromosomePloidyFile(File predictedSexChromosomePloidy) throws IOException {
        String contents = FileUtils.readFileToString(predictedSexChromosomePloidy);
        return contents.replaceAll("Predicted sex chromosome ploidy", "").trim();
    }

    public AlignmentDataFiles parseStats(File outputDirectory, String filePrefix, DragenReplayInfo dragenReplayInfo,
                                         MessageCollection messageCollection,
                                         Map<String, String> mapReadGroupToSampleAlias,
                                         Pair<String, String> runNameDatePair) {
        try {
            File averageCoverageMetrics = new File(outputDirectory, filePrefix + ".qc-coverage-region-1_overall_mean_cov.csv");
            Float meanCoverage = MeanCoverageParser
                    .parseMeanCoverage(new FileInputStream(averageCoverageMetrics), messageCollection);
            if (meanCoverage == null) {
                String err = "Failed to parse mean coverage " + averageCoverageMetrics.getPath();
                log.error(err);
                messageCollection.addError(err);
            }

            File mappingMetricsFile = new File(outputDirectory, filePrefix + ".mapping_metrics.csv");
            File mappingMetricsOutputFile = new File(outputDirectory, filePrefix + ".mapping_metrics_mercury.dat");
            File mappingSummaryOutputFile = new File(outputDirectory, filePrefix + ".mapping_summary_mercury.dat");
            if (mappingMetricsFile.exists()) {
                parseMappingMetrics(mappingMetricsFile, mappingSummaryOutputFile, mappingMetricsOutputFile,
                        runNameDatePair.getLeft(), new Date(), dragenReplayInfo, mapReadGroupToSampleAlias,
                        runNameDatePair.getRight(), meanCoverage, false);
            } else {
                messageCollection.addError("Failed to find mapping metrics file " + mappingMetricsFile.getPath());
            }

            File vcMetricsFile = new File(outputDirectory, filePrefix + ".vc_metrics.csv");
            File vcMetricsOutputFile = new File(outputDirectory, filePrefix + ".vc_metrics_mercury.dat");
            File vcSummaryOutputFile = new File(outputDirectory, filePrefix + ".vc_summary_mercury.dat");
            if (vcMetricsFile.exists()) {
                parseVcMetrics(vcMetricsFile, vcSummaryOutputFile, vcMetricsOutputFile, runNameDatePair.getLeft(),
                        new Date(), dragenReplayInfo,  runNameDatePair.getRight(), false);
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
                                DragenReplayInfo dragenReplayInfo, String analysisName, boolean summaryOnly) throws IOException {
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

        if (summaryOnly) {
            return;
        }

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
                                     Map<String, String> mapReadGroupToSampleAlias, String analysisName,
                                     Float seqCoverage, boolean summaryOnly) throws IOException {
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
                if (recordField.equals(SEQ_COVERAGE_FIELD)) {
                    recordValue = String.valueOf(seqCoverage);
                }
                alignmentSummary.add(recordValue);
                mapFieldToVal.put(recordField, recordValue);
            }
        }

        csvWriterSummary.writeNext(alignmentSummary.toArray(new String[0]));
        csvWriterSummary.close();

        if (summaryOnly) {
            return;
        }

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
            recordField = nextRecord[2];

            // TODO Do I still want individuals on aggregation?
            if (recordField.equals(SEQ_COVERAGE_FIELD)) {
                recordValue = String.valueOf(seqCoverage);
            }
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

    public enum IncludeRate {
        TRUE(true),
        FALSE(false);
        private final boolean value;

        IncludeRate(boolean value) {
            this.value = value;
        }

        public boolean booleanValue() {
            return value;
        }
    }

    public enum OptionalRecord {
        TRUE(true),
        FALSE(false);
        private final boolean value;

        OptionalRecord(boolean value) {
            this.value = value;
        }

        public boolean booleanValue() {
            return value;
        }
    }

    public interface RecordType {
        String getRecordType();
        boolean getIncludeRate();
        boolean isOptional();
    }

    public enum SummaryFields implements RecordType {
        TOTAL_READS("Total input reads"),
        NUM_DUPE_MARKED_READS("Number of duplicate marked reads"),
        NUM_DUPE_MARKED_READS_REMOVED("Number of duplicate marked and mate reads removed"),
        NUM_UNIQUE_READS_EXCL("Number of unique reads (excl. duplicate marked reads)"),
        READS_WITH_MATE_SEQ("Reads with mate sequenced"),
        READS_WITHOUT_MATE_SEQ("Reads without mate sequenced"),
        QC_FAILED_READS("QC-failed reads"),
        MAPPED_READS("Mapped reads"),
        MAPPED_READS_R1("Mapped reads R1"),
        MAPPED_READS_R2("Mapped reads R2"),
        NUM_UNQ_MAPPED_READS("Number of unique & mapped reads (excl. duplicate marked reads)"),
        UNMAPPED_READS("Unmapped reads"),
        SINGLETON_READS("Singleton reads (itself mapped; mate unmapped)"),
        PAIRED_READS("Paired reads (itself & mate mapped)"),
        PROPERLY_PAIRED_READS("Properly paired reads"),
        NOT_PROPERLY_PAIRED_READS("Not properly paired reads (discordant)"),
        PAIRED_READS_DIFF_CHROM("Paired reads mapped to different chromosomes"),
        PAIRED_READS_DIFF_CHROM_Q10("Paired reads mapped to different chromosomes (MAPQ>=10)"),
        READS_MAPQ_40_INF("Reads with MAPQ [40:inf)"),
        READS_MAPQ_30_40("Reads with MAPQ [30:40)"),
        READS_MAPQ_20_30("Reads with MAPQ [20:30)"),
        READS_MAPQ_10_20("Reads with MAPQ [10:20)"),
        READS_MAPQ_0_10("Reads with MAPQ [ 0:10)"),
        READS_MAPQ_NA("Reads with MAPQ NA (Unmapped reads)"),
        READS_INDEL_R1("Reads with indel R1"),
        READS_INDEL_R2("Reads with indel R2"),
        TOTAL_BASES("Total bases"),
        TOTAL_BASES_R1("Total bases R1"),
        TOTAL_BASES_R2("Total bases R2"),
        MAPPED_BASES_R1("Mapped bases R1"),
        MAPPED_BASES_R2("Mapped bases R2"),
        SOFT_CLIPPED_R1("Soft-clipped bases R1"),
        SOFT_CLIPPED_R2("Soft-clipped bases R2"),
        MISMATCHED_BASES_R1("Mismatched bases R1"),
        MISMATCHED_BASES_R2("Mismatched bases R2"),
        MISMATCHED_BASES_R1_EXCL("Mismatched bases R1 (excl. indels)"),
        MISMATCHED_BASES_R2_EXCL("Mismatched bases R2 (excl. indels)"),
        Q30_BASES("Q30 bases"),
        Q30_BASES_R1("Q30 bases R1"),
        Q30_BASES_R2("Q30 bases R2"),
        Q30_BASES_EXCL("Q30 bases (excl. dups & clipped bases)"),
        TOTAL_ALIGNMENTS("Total alignments"),
        SECONDARY_ALIGNMENTS("Secondary alignments"),
        SUPPLEMENTARY_ALIGNMENTS("Supplementary (chimeric) alignments"),
        EST_READ_LENGTH("Estimated read length"),
        AVG_SEQ_COV_OVER_GENOME("Average sequenced coverage over genome"),
        BASES_IN_REF_GENOME("Bases in reference genome"),
        BASES_IN_TARGET_BED("Bases in target bed [% of genome]"),
        INSERT_LENGTH_MEAN("Insert length: mean"),
        INSERT_LENGTH_MED("Insert length: median"),
        INSERT_LENGTH_STD("Insert length: standard deviation"),
        PROVIDED_SEX_CHROM_PLOIDY("Provided sex chromosome ploidy"),
        EST_SAMPLE_CONTAM("Estimated sample contamination"),
        MAPPING_RATE("DRAGEN mapping rate [mil. reads/second]"),
        ;

        private final String recordType;
        private static final Map<String, SummaryFields> mapFieldToSummary = new HashMap<>();

        static {
            for (SummaryFields summaryFields: SummaryFields.values()) {
                mapFieldToSummary.put(summaryFields.getRecordType(), summaryFields);
            }
        }

        SummaryFields(String recordType) {
            this.recordType = recordType;
        }

        @Override
        public String getRecordType() {
            return recordType;
        }

        @Override
        public boolean getIncludeRate() {
            return false;
        }

        @Override
        public boolean isOptional() {
            return false;
        }

        public static SummaryFields getRecordFromField(String field) {
            return mapFieldToSummary.get(field);
        }
    }

    public enum RgFields implements RecordType {
        TOTAL_READS("Total reads in RG"),
        NUM_DUPE_MARKED_READS("Number of duplicate marked reads"),
        NUM_DUPE_MARKED_READS_REMOVED("Number of duplicate marked and mate reads removed"),
        NUM_UNIQUE_READS_EXCL("Number of unique reads (excl. duplicate marked reads)"),
        READS_WITH_MATE_SEQ("Reads with mate sequenced"),
        READS_WITHOUT_MATE_SEQ("Reads without mate sequenced"),
        QC_FAILED_READS("QC-failed reads"),
        MAPPED_READS("Mapped reads"),
        MAPPED_READS_R1("Mapped reads R1"),
        MAPPED_READS_R2("Mapped reads R2"),
        NUM_UNQ_MAPPED_READS("Number of unique & mapped reads (excl. duplicate marked reads)"),
        UNMAPPED_READS("Unmapped reads"),
        SINGLETON_READS("Singleton reads (itself mapped; mate unmapped)"),
        PAIRED_READS("Paired reads (itself & mate mapped)"),
        PROPERLY_PAIRED_READS("Properly paired reads"),
        NOT_PROPERLY_PAIRED_READS("Not properly paired reads (discordant)"),
        PAIRED_READS_DIFF_CHROM("Paired reads mapped to different chromosomes"),
        PAIRED_READS_DIFF_CHROM_Q10("Paired reads mapped to different chromosomes (MAPQ>=10)"),
        READS_MAPQ_40_INF("Reads with MAPQ [40:inf)"),
        READS_MAPQ_30_40("Reads with MAPQ [30:40)"),
        READS_MAPQ_20_30("Reads with MAPQ [20:30)"),
        READS_MAPQ_10_20("Reads with MAPQ [10:20)"),
        READS_MAPQ_0_10("Reads with MAPQ [ 0:10)"),
        READS_MAPQ_NA("Reads with MAPQ NA (Unmapped reads)"),
        READS_INDEL_R1("Reads with indel R1"),
        READS_INDEL_R2("Reads with indel R2"),
        TOTAL_BASES("Total bases"),
        TOTAL_BASES_R1("Total bases R1"),
        TOTAL_BASES_R2("Total bases R2"),
        MAPPED_BASES_R1("Mapped bases R1"),
        MAPPED_BASES_R2("Mapped bases R2"),
        SOFT_CLIPPED_R1("Soft-clipped bases R1"),
        SOFT_CLIPPED_R2("Soft-clipped bases R2"),
        MISMATCHED_BASES_R1("Mismatched bases R1"),
        MISMATCHED_BASES_R2("Mismatched bases R2"),
        MISMATCHED_BASES_R1_EXCL("Mismatched bases R1 (excl. indels)"),
        MISMATCHED_BASES_R2_EXCL("Mismatched bases R2 (excl. indels)"),
        Q30_BASES("Q30 bases"),
        Q30_BASES_R1("Q30 bases R1"),
        Q30_BASES_R2("Q30 bases R2"),
        Q30_BASES_EXCL("Q30 bases (excl. dups & clipped bases)"),
        TOTAL_ALIGNMENTS("Total alignments"),
        SECONDARY_ALIGNMENTS("Secondary alignments"),
        SUPPLEMENTARY_ALIGNMENTS("Supplementary (chimeric) alignments"),
        EST_READ_LENGTH("Estimated read length"),
        AVG_SEQ_COV_OVER_GENOME("Average sequenced coverage over genome"),
        INSERT_LENGTH_MEAN("Insert length: mean"),
        INSERT_LENGTH_MED("Insert length: median"),
        INSERT_LENGTH_STD("Insert length: standard deviation"),
        ;

        private final String recordType;

        private static final Map<String, RgFields> mapFieldToCoverage = new HashMap<>();

        static {
            for (RgFields summaryFields: RgFields.values()) {
                mapFieldToCoverage.put(summaryFields.getRecordType(), summaryFields);
            }
        }

        RgFields(String recordType) {
            this.recordType = recordType;
        }

        @Override
        public String getRecordType() {
            return recordType;
        }

        public static RgFields getRecordFromField(String field) {
            return mapFieldToCoverage.get(field);
        }

        @Override
        public boolean getIncludeRate() {
            return false;
        }

        @Override
        public boolean isOptional() {
            return false;
        }
    }

    public enum CoverageFields implements RecordType {
        ALIGNED_BASES("Aligned bases"),
        ALIGNED_BASES_IN_REGION("Aligned bases in QC coverage region"),
        AVG_ALIGNMENT_COVERAGE("Average alignment coverage over QC coverage region"),
        UNIFORMITY_OF_COV("Uniformity of coverage (PCT > 0.2*mean) over QC coverage region"),
        PCT_COV_100X_INF("PCT of QC coverage region with coverage [100x: inf)"),
        PCT_COV_50X_INF("PCT of QC coverage region with coverage [ 50x: inf)"),
        PCT_COV_20X_INF("PCT of QC coverage region with coverage [ 20x: inf)"),
        PCT_COV_15X_INF("PCT of QC coverage region with coverage [ 15x: inf)"),
        PCT_COV_10X_INF("PCT of QC coverage region with coverage [ 10x: inf)"),
        PCT_COV_3X_INF("PCT of QC coverage region with coverage [  3x: inf)"),
        PCT_COV_1X_INF("PCT of QC coverage region with coverage [  1x: inf)"),
        PCT_COV_0X_INF("PCT of QC coverage region with coverage [  0x: inf)"),
        PCT_COV_50X_100X("PCT of QC coverage region with coverage [ 50x:100x)"),
        PCT_COV_20X_50X("PCT of QC coverage region with coverage [ 20x: 50x)"),
        PCT_COV_15X_20X("PCT of QC coverage region with coverage [ 15x: 20x)"),
        PCT_COV_10X_15X("PCT of QC coverage region with coverage [ 10x: 15x)"),
        PCT_COV_3X_10X("PCT of QC coverage region with coverage [  3x: 10x)"),
        PCT_COV_1X_3X("PCT of QC coverage region with coverage [  1x:  3x)"),
        PCT_COV_0X_1X("PCT of QC coverage region with coverage [  0x:  1x)"),
        AVG_CHR_X_COV("Average chr X coverage over QC coverage region"),
        AVG_CHR_Y_COV("Average chr Y coverage over QC coverage region"),
        AVG_MITO_COV("Average mitochondrial coverage over QC coverage region"),
        AVG_AUTOSOMAL_COV("Average autosomal coverage over QC coverage region"),
        MED_AUTOSOMAL_COV("Median autosomal coverage over QC coverage region"),
        MEAN_MED_AUTO_COV("Mean/Median autosomal coverage ratio over QC coverage region"),
        XAVG_YAVG_COV("XAvgCov/YAvgCov ratio over QC coverage region"),
        XAVG_AUTOSOMAL_COV("XAvgCov/AutosomalAvgCov ratio over QC coverage region"),
        YAVG_AUTOSOMAL_COV("YAvgCov/AutosomalAvgCov ratio over QC coverage region"),
        ALIGNED_READS("Aligned reads"),
        ALIGNED_READS_QC("Aligned reads in QC coverage region"),
        ;

        private final String recordType;

        private static final Map<String, CoverageFields> mapFieldToCoverage = new HashMap<>();

        static {
            for (CoverageFields field: CoverageFields.values()) {
                mapFieldToCoverage.put(field.getRecordType(), field);
            }
        }

        CoverageFields(String recordType) {
            this.recordType = recordType;
        }

        @Override
        public String getRecordType() {
            return recordType;
        }

        public static CoverageFields getRecordFromField(String field) {
            return mapFieldToCoverage.get(field);
        }

        @Override
        public boolean getIncludeRate() {
            return false;
        }

        @Override
        public boolean isOptional() {
            return false;
        }
    }

    public enum VcFields implements RecordType {
        NUM_SAMPLES("Number of samples", IncludeRate.FALSE),
        READS_PROCESSED("Reads Processed", IncludeRate.FALSE),
        CHILD_SAMPLE("Child Sample", IncludeRate.FALSE),
        TOTAL("Total"),
        BIALLELIC("Biallelic"),
        MULTIALLELIC("Multiallelic"),
        SNPS("SNPs"),
        INSERTIONS_HOM("Insertions (Hom)"),
        INSERTIONS_HET("Insertions (Het)"),
        DELETIONS_HOM("Deletions (Hom)"),
        DELETIONS_HET("Deletions (Het)"),
        INDELS_HET("Indels (Het)"),
        CHR_X("Chr X number of SNPs over genome", IncludeRate.FALSE),
        CHR_Y("Chr Y number of SNPs over genome", IncludeRate.FALSE),
        CHR_XY_RATIO("(Chr X SNPs)/(chr Y SNPs) ratio over genome", IncludeRate.FALSE),
        SNP_TRANSITIONS("SNP Transitions", IncludeRate.FALSE),
        SNP_TRANSVERSIONS("SNP Transversions", IncludeRate.FALSE),
        TI_TV_RATIO("Ti/Tv ratio", IncludeRate.FALSE),
        HET("Heterozygous", IncludeRate.FALSE),
        HOM("Homozygous", IncludeRate.FALSE),
        HET_HOM_RATIO("Het/Hom ratio", IncludeRate.FALSE),
        IN_DBSNP("In dbSNP"),
        NOT_IN_DBSNP("Not in dbSNP"),
        PERCENT_CALLABILITY("Percent Callability", IncludeRate.FALSE, OptionalRecord.TRUE),
        PERCENT_AUTOSOME_CALLABILITY("Percent Autosome Callability", IncludeRate.FALSE, OptionalRecord.TRUE),
        PERCENT_AUTOSOME_EXOME_CALLABILITY("Percent Autosome Exome Callability", IncludeRate.FALSE, OptionalRecord.TRUE),
        ;

        private final String recordType;
        private final IncludeRate includeRate;
        private final OptionalRecord optionalRecord;
        private static final Map<String, AlignmentStatsParser.VcFields> mapFieldToSummary = new HashMap<>();

        static {
            for (AlignmentStatsParser.VcFields fields: AlignmentStatsParser.VcFields.values()) {
                mapFieldToSummary.put(fields.getRecordType(), fields);
            }
        }

        VcFields(String recordType) {
            this(recordType, IncludeRate.TRUE);
        }

        VcFields(String recordType, IncludeRate includeRate) {
            this(recordType, includeRate, OptionalRecord.FALSE);
        }

        VcFields(String recordType, IncludeRate includeRate, OptionalRecord optionalRecord) {
            this.recordType = recordType;
            this.includeRate = includeRate;
            this.optionalRecord = optionalRecord;
        }

        @Override
        public String getRecordType() {
            return recordType;
        }

        public static AlignmentStatsParser.VcFields getRecordFromField(String field) {
            return mapFieldToSummary.get(field);
        }

        @Override
        public boolean getIncludeRate() {
            return includeRate == IncludeRate.TRUE;
        }

        @Override
        public boolean isOptional() {
            return optionalRecord == OptionalRecord.TRUE;
        }
    }
}
