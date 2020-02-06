package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Transient;
import java.io.File;

/**
 * Represents a task that should be run using Bcl2Fastq2 software
 */
@Entity
@Audited
public class BclDemultiplexTask extends ComputeTask {

    @Transient
    private File inputDirectory;

    @Transient
    private File outputDirectory;

    @Transient
    private File sampleSheet;

    public BclDemultiplexTask(File inputDirectory, File outputDirectory, File sampleSheet) {
        super();
        this.inputDirectory = inputDirectory;
        this.outputDirectory = outputDirectory;
        this.sampleSheet = sampleSheet;

        String cmd = new Bcl2FastqTaskBuilder().
                bclInputDirectory(inputDirectory).
                outputDirectory(outputDirectory).
                sampleSheet(sampleSheet).
                build();

        setCommandLineArgument(cmd);
    }

    public BclDemultiplexTask() {
    }
}
