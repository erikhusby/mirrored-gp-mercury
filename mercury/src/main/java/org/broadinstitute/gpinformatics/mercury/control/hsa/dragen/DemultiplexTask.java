package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Table;
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
        this.bclInputDirectory = bclInputDirectory;
        this.outputDirectory = outputDirectory;
        this.sampleSheet = sampleSheet;
        Objects.requireNonNull(bclInputDirectory, "bcl Input directory must not be null.");
        Objects.requireNonNull(outputDirectory, "Output directory must not be null.");
        Objects.requireNonNull(sampleSheet, "Sample Sheet must not be null.");

        if (!bclInputDirectory.exists()) {
            throw new IllegalArgumentException("bclInputDirectory must exist.");
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
        return bclInputDirectory;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public File getSampleSheet() {
        return sampleSheet;
    }
}
