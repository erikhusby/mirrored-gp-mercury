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

    public static final int NUM_PROCESSING_THREADS = 12;
    @Transient
    private File runDirectory;

    @Transient
    private File outputDirectory;

    @Transient
    private File sampleSheet;

    public BclDemultiplexTask(File runDirectory, File outputDirectory, File sampleSheet) {
        super();
        this.runDirectory = runDirectory;
        this.outputDirectory = outputDirectory;
        this.sampleSheet = sampleSheet;

        String cmd = new Bcl2FastqTaskBuilder().
                runFolderDirectory(runDirectory).
                outputDirectory(outputDirectory).
                sampleSheet(sampleSheet).
                numberOfLoadingThreads(4).
                numberOfWritingThreads(4).
                numberOfProcessingThreads(NUM_PROCESSING_THREADS).
                build();

        setCommandLineArgument(cmd);
    }

    public BclDemultiplexTask() {
    }

    @Override
    public boolean isExclusive() {
        return false;
    }

    @Override
    public boolean hasCpuPerTaskLimit() {
        return true;
    }

    /**
     * Testing with 12 since the node currently has 24 cpus. A Blog post claims that 16 is optimal with 4 and 4 r+w threads
     */
    @Override
    public int getCpusPerTask() {
        return NUM_PROCESSING_THREADS;
    }

    public File getRunDirectory() {
        if (runDirectory == null) {
            runDirectory = new File(DragenTaskBuilder.parseCommandFromArgument(
                    Bcl2FastqTaskBuilder.RUN_FOLDER_DIRECTORY, getCommandLineArgument()));
        }
        return runDirectory;
    }

    public File getOutputDirectory() {
        if (outputDirectory == null) {
            outputDirectory = new File(DragenTaskBuilder.parseCommandFromArgument(
                    Bcl2FastqTaskBuilder.OUTPUT_DIRECTORY, getCommandLineArgument()));
        }
        return outputDirectory;
    }
}
