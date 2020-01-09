package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.control.hsa.SampleSheetBuilder;
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.AlignmentStatsParser;
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.DemultiplexStats;
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.DragenReplayInfo;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.DemultiplexState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.ReadGroupUtil;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.TaskResult;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;

import static org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme.IndexPosition.ILLUMINA_P5;
import static org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme.IndexPosition.ILLUMINA_P7;

public class DragenSimulator implements Dragen {

    private static final Log log = LogFactory.getLog(DragenSimulator.class);

    @Override
    public TaskResult fireProcess(String commandLine, Task task) {
        log.info("Dragen() " + commandLine);
        try {
            if (task instanceof DemultiplexTask) {
                return handleDemultiplex((DemultiplexTask) task);
            } else if (task instanceof AlignmentTask) {
                return handleAlignment((AlignmentTask) task);
            } else if (task instanceof AggregationTask) {
                return handleAggregationTask((AggregationTask) task);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    private TaskResult handleAggregationTask(AggregationTask task) throws IOException {
        task.getOutputDir().mkdir();
        String sampleId = task.getFastQSampleId();
        File replayFile = new File(task.getOutputDir(), sampleId + "-replay.json");
        createReplayFile(replayFile);
        File mappingFile = new File(task.getOutputDir(), sampleId + ".mapping_metrics.csv");
        createMappingFile(mappingFile, sampleId);

        File ploidyFile = new File(task.getOutputDir(), sampleId + ".wgs_ploidy.csv");
        createPloidyFile(ploidyFile);

        File covFile = new File(task.getOutputDir(), sampleId + ".qc-coverage-region-1_coverage_metrics.csv");
        createCoverageFile(covFile);

        File vcFile = new File(task.getOutputDir(), sampleId + ".vc_metrics.csv");
        createVcFile(vcFile);

        return new TaskResult(new Random().nextLong(), "Success", 0);
    }

    private void createVcFile(File vcFile) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (AlignmentStatsParser.VcFields fields: AlignmentStatsParser.VcFields.values()) {
            switch (fields) {

            case NUM_SAMPLES:
            case READS_PROCESSED:
            case CHILD_SAMPLE:
                sb.append("VARIANT CALLER SUMMARY,,").append(fields.getRecordType());
                sb.append(",");
                sb.append("1");
                break;
            default:
                sb.append("VARIANT CALLER POSTFILTER,,").append(fields.getRecordType());
                sb.append(",");
                sb.append("1");
                break;
            }
            sb.append("\n");
        }
        FileUtils.writeStringToFile(vcFile, sb.toString());
    }

    private void createCoverageFile(File covFile) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (AlignmentStatsParser.CoverageFields fields: AlignmentStatsParser.CoverageFields.values()) {
            sb.append("COVERAGE SUMMARY,,").append(fields.getRecordType());
            sb.append(",");

            if (fields == AlignmentStatsParser.CoverageFields.AVG_ALIGNMENT_COVERAGE) {
                sb.append("28.2");
            } else {
                sb.append("100");
            }
            sb.append("\n");
        }
        FileUtils.writeStringToFile(covFile, sb.toString());
    }

    private void createPloidyFile(File ploidyFile) throws IOException {
        FileUtils.writeStringToFile(ploidyFile, "Predicted sex chromosome ploidy XX");
    }

    // TODO Create better mapping
    private void createMappingFile(File mappingFile, String rgId) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (AlignmentStatsParser.SummaryFields summaryFields: AlignmentStatsParser.SummaryFields.values()) {
            sb.append("MAPPING/ALIGNING SUMMARY,,").append(summaryFields.getRecordType());
            sb.append(",");
            sb.append("100");
            sb.append("\n");
        }
        for (AlignmentStatsParser.RgFields rgFields: AlignmentStatsParser.RgFields.values()) {
            sb.append("MAPPING/ALIGNING PER RG,").append(rgId).append(",").append(rgFields.getRecordType());
            sb.append(",");
            sb.append("100");
            sb.append("\n");
        }
        FileUtils.writeStringToFile(mappingFile, sb.toString());
    }

    // TODO write alignment files for testing coverage
    private TaskResult handleAlignment(AlignmentTask task) {
        File fastQ = task.getFastQList();

        return new TaskResult(new Random().nextLong(), "Success", 0);
    }

    /**
     * Produces FASTQ files for each sample and lane. Demultiplexes based on the sample sheet into the specified
     * output directory
     * @param task - task that defines the demultiplex action.
     */
    private TaskResult handleDemultiplex(DemultiplexTask task) throws IOException {
        File sampleSheet = task.getSampleSheet();

        if (sampleSheet == null || !sampleSheet.exists()) {
            return new TaskResult(new Random().nextLong(), "Success", 0);
        }

        File outputDir = task.getOutputDirectory();
        File reportsDir = new File(outputDir, "Reports");

        reportsDir.mkdirs();

        StringBuilder sb = new StringBuilder();
        sb.append("RGID,RGSM,RGLB,Lane,Read1File,Read2File").append("\n");
        Map<String, File> sampleNameToOutputDir = new HashMap<>();
        // TODO Get from dao the Mercury SAmple
        DemultiplexState state = (DemultiplexState) task.getState();

        for (SampleSheetBuilder.SampleData sampleData: SampleSheetBuilder.grabDataFromFile(sampleSheet)) {
            // Sample Data is reall the import
            String sampleName = sampleData.getSampleName();
            String rgId = ReadGroupUtil.createSampleSheetId(task.getOutputDirectory().getName(), sampleData.getLane(),
                    sampleName);

            String r1Fastq = "R1 fastq";
            String r2Fastq = "R2 fastq";
            sb.append(rgId).append(",").
                    append(rgId).append(",").
                    append(rgId).append(",").
                    append(sampleData.getLane()).
                    append(",").append(r1Fastq).append(",").
                    append(r2Fastq).append("\n");

            File alignmentOutputDir = new File(outputDir, sampleName);
            alignmentOutputDir.mkdir();

//            createAlignmentAndVariantCallingMetrics(alignmentOutputDir, sampleData, rgId);
            sampleNameToOutputDir.put(sampleName, alignmentOutputDir);
        }

        try {
            File fastqFile = new File(reportsDir, "fastq_list.csv");
            FileUtils.writeStringToFile(fastqFile, sb.toString());

            File demuxStats = new File(reportsDir, "Demultiplex_Stats.csv");
            createDemultiplexMetricsFile(state.getRun(), demuxStats);

            File replayFile = new File(outputDir, "replay.json");
            createReplayFile(replayFile);

            for (Map.Entry<String, File> entry: sampleNameToOutputDir.entrySet()) {
                createReplayFile(new File(entry.getValue(), entry.getKey() + "-replay.json"));
            }

            long pid = new Random().nextLong();
            return new TaskResult(pid, "Success", 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createAlignmentAndVariantCallingMetrics(File alignmentOutputDir,
                                                         SampleSheetBuilder.SampleData sampleData,
                                                         String rgId) throws IOException {
        File mappingMetrics = new File(alignmentOutputDir, sampleData.getSampleName() + ".mapping_metrics.csv");
        String mappingMetricsTestData = IOUtils.toString(
                getTestResource("dragen/TCGA-CF-A9FH-01A-11D-A38G-08.mapping_metrics.csv"));
        mappingMetricsTestData = mappingMetricsTestData.replaceAll("CAATTAAC.CGAGATAT.2", rgId);
        FileUtils.writeStringToFile(mappingMetrics, mappingMetricsTestData);

        File vcMetrics = new File(alignmentOutputDir, sampleData.getSampleName() + ".vc_metrics.csv");
        String vcMetricsTestData = IOUtils.toString(
                getTestResource("dragen/TCGA-CF-A9FH-01A-11D-A38G-08.vc_metrics.csv"));
        vcMetricsTestData = vcMetricsTestData.replaceAll("TCGA-CF-A9FH-01A-11D-A38G-08", sampleData.getSampleName());
        FileUtils.writeStringToFile(vcMetrics, vcMetricsTestData);
    }

    private static void createDemultiplexMetricsFile(IlluminaSequencingRun run, File outputFile) throws IOException {
        RunCartridge flowcell = run.getSampleCartridge();
        VesselPosition[] positionNames = flowcell.getVesselGeometry().getVesselPositions();
        List<DemultiplexStats> dtos = new ArrayList<>();
        int laneNum = 0;
        for (VesselPosition vesselPosition: positionNames) {
            ++laneNum;
            for (SampleInstanceV2 sampleInstanceV2 : flowcell.getContainerRole().getSampleInstancesAtPositionV2(vesselPosition)) {
                MolecularIndexingScheme molecularIndexingScheme = sampleInstanceV2.getMolecularIndexingScheme();
                SortedMap<MolecularIndexingScheme.IndexPosition, MolecularIndex> indexes =
                        molecularIndexingScheme.getIndexes();
                String index = indexes.get(ILLUMINA_P7).getSequence() + "-" + indexes.get(ILLUMINA_P5).getSequence();

                DemultiplexStats stats = new DemultiplexStats();
                stats.setIndex(index);
                stats.setLane(laneNum);
                stats.setMeanQualityScorePassingFilter("35.42");
                stats.setNumberOfReads(1000);
                stats.setNumberOfOneMismatchIndexreads(100);
                stats.setNumberOfPerfectIndexReads(900);
                stats.setNumberOfQ30BasesPassingFilter(new BigDecimal("4000"));
                String sampleKey = sampleInstanceV2.getNearestMercurySampleName();
                String sampleId = ReadGroupUtil.createSampleSheetId(flowcell.getLabel(), laneNum, sampleKey);
                stats.setSampleID(sampleId);
                dtos.add(stats);
            }
        }
        System.out.println("Making demux file " + outputFile.getPath());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            StatefulBeanToCsv<DemultiplexStats> beanToCsv =
                    new StatefulBeanToCsvBuilder<DemultiplexStats>(writer)
                            .withSeparator(',')
                            .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                            .build();
            beanToCsv.write(dtos);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void createReplayFile(File outputFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        DragenReplayInfo dragenReplayInfo = new DragenReplayInfo();
        dragenReplayInfo.setSystem(new DragenReplayInfo.System());
        dragenReplayInfo.getSystem().setDragenVersion("01.011.308.3.3.7");
        dragenReplayInfo.getSystem().setKernelRelease("3.10.0-862.6.3.el7.x86_64");
        dragenReplayInfo.getSystem().setNodename("dragen01");
        mapper.writeValue(sw, dragenReplayInfo);
        FileUtils.writeStringToFile(outputFile, sw.toString());
    }

    public static InputStream getSpreadsheet(String filename) {
        return getTestResource(filename);
    }

    public static InputStream getTestResource(String fileName) {
        InputStream testSpreadSheetInputStream = getResourceAsStream(fileName);
        if (testSpreadSheetInputStream == null) {
            testSpreadSheetInputStream = getResourceAsStream("testdata/" + fileName);
        }
        return testSpreadSheetInputStream;
    }

    public static InputStream getResourceAsStream(String fileName) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
    }
}
