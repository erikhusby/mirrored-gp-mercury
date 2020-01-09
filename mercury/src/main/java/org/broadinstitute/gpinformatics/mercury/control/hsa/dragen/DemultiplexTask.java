package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Transient;
import java.io.File;
import java.util.Objects;

@Entity
@Audited
public class DemultiplexTask extends ProcessTask {

    @Transient
    private File bclInputDirectory;

    @Transient
    private File outputDirectory;

    @Transient
    private File sampleSheet;

    /**
     * dragen --bcl-conversion-only=true \
     * --bcl-input-directory /seq/illumina/proc/<instrument>/<runfolder> \
     * --output-directory /path/to/output/directory \
     * --sample-sheet /seq/illumina/proc/<instrument>/<runfolder>/<SampleSheet.csv>
     */
    public DemultiplexTask(File bclInputDirectory, File outputDirectory, File sampleSheet) {
        super("dragen");
        this.bclInputDirectory = bclInputDirectory;
        this.outputDirectory = outputDirectory;
        this.sampleSheet = sampleSheet;
        Objects.requireNonNull(bclInputDirectory, "bcl Input directory must not be null.");
        Objects.requireNonNull(outputDirectory, "Output directory must not be null.");
        Objects.requireNonNull(sampleSheet, "Sample Sheet must not be null.");

        if (!bclInputDirectory.exists()) {
            throw new IllegalArgumentException("bclInputDirectory must exist: " + bclInputDirectory.getPath());
        }

        String dragenTaskBuilder = new DragenTaskBuilder().
                bclConversionOnly(true).
                bclInputDirectory(bclInputDirectory).
                outputDirectory(outputDirectory).
                sampleSheet(sampleSheet).build();

        setCommandLineArgument(dragenTaskBuilder);
    }

    public DemultiplexTask() {
    }

    public File getBclInputDirectory() {
        if (bclInputDirectory == null) {
            bclInputDirectory = new File(DragenTaskBuilder.parseCommandFromArgument(
                    DragenTaskBuilder.BCL_INPUT_DIRECTORY, getCommandLineArgument()));
        }
        return bclInputDirectory;
    }

    public File getOutputDirectory() {
        if (outputDirectory == null) {
            outputDirectory = new File(
                    DragenTaskBuilder.parseCommandFromArgument(DragenTaskBuilder.OUTPUT_DIRECTORY, getCommandLineArgument()));
        }
        return outputDirectory;
    }

    public File getSampleSheet() {
        if (sampleSheet == null) {
            sampleSheet = new File(
                    DragenTaskBuilder.parseCommandFromArgument(DragenTaskBuilder.SAMPLE_SHEET, getCommandLineArgument()));
        }
        return sampleSheet;
    }
}
