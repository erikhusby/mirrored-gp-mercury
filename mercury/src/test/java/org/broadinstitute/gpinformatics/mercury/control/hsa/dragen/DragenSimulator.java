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
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.DemultiplexStats;
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.DragenReplayInfo;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.DemultiplexState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.ReadGroupUtil;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.TaskResult;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
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
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return null;
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
        for (SampleSheetBuilder.SampleData sampleData: SampleSheetBuilder.grabDataFromFile(sampleSheet)) {
            String rgId = ReadGroupUtil.createSampleSheetId(task.getOutputDirectory().getName(), sampleData.getLane(),
                    sampleData.getSampleName());
            String r1Fastq = "R1 fastq";
            String r2Fastq = "R2 fastq";
            sb.append(rgId).append(",").append(sampleData.getSampleName()).append(",").
                    append(sampleData.getLane()).append(",").append(r1Fastq).append(",").
                    append(r2Fastq).append("\n");

            File alignmentOutputDir = new File(outputDir, sampleData.getSampleName());
            alignmentOutputDir.mkdir();

            createAlignmentAndVariantCallingMetrics(alignmentOutputDir, sampleData, rgId);
            sampleNameToOutputDir.put(sampleData.getSampleName(), alignmentOutputDir);
        }

        try {
            File fastqFile = new File(reportsDir, "fastq_list.csv");
            FileUtils.writeStringToFile(fastqFile, sb.toString());

            DemultiplexState state = (DemultiplexState) task.getState();
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
                VarioskanParserTest.getTestResource("dragen/TCGA-CF-A9FH-01A-11D-A38G-08.mapping_metrics.csv"));
        mappingMetricsTestData = mappingMetricsTestData.replaceAll("CAATTAAC.CGAGATAT.2", rgId);
        FileUtils.writeStringToFile(mappingMetrics, mappingMetricsTestData);

        File vcMetrics = new File(alignmentOutputDir, sampleData.getSampleName() + ".vc_metrics.csv");
        String vcMetricsTestData = IOUtils.toString(
                VarioskanParserTest.getTestResource("dragen/TCGA-CF-A9FH-01A-11D-A38G-08.vc_metrics.csv"));
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
                String sampleId = ReadGroupUtil.createSampleSheetId(flowcell.getLabel(), laneNum,
                        sampleInstanceV2.getRootOrEarliestMercurySampleName());
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
}
