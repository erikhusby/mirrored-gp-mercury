package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.control.hsa.SampleSheetBuilder;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.TaskResult;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

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

        // TODO write any more real files other than fastq
        StringBuilder sb = new StringBuilder();
        sb.append("RGID,RGSM,RGLB,Lane,Read1File,Read2File");
        for (SampleSheetBuilder.SampleData sampleData: SampleSheetBuilder.grabDataFromFile(sampleSheet)) {
            String rgId = String.format("%s.%s.%d", sampleData.getIndex(), sampleData.getIndex2(), sampleData.getLane());
            String r1Fastq = "R1 fastq";
            String r2Fastq = "R2 fastq";
            sb.append(rgId).append(",").append(sampleData.getSampleName()).append(",").
                    append(sampleData.getLane()).append(",").append(r1Fastq).append(",").
                    append(r2Fastq);
        }

        try {
            File fastqFile = new File(reportsDir, "fastq_list.csv");
            FileUtils.writeStringToFile(fastqFile, sb.toString());

            long pid = new Random().nextLong();
            return new TaskResult(pid, "Success", 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
