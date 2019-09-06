package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.broadinstitute.gpinformatics.infrastructure.deployment.DragenConfig;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;

import java.io.File;

public class DragenFolderUtil {

    private final DragenConfig dragenConfig;
    private final IlluminaSequencingRun illuminaSequencingRun;
    private final String analysisKey;
    private final File replayJsonFile;
    private final File fastQListFile;

    private File runFolder;
    private File dragenFolder;
    private File analysisFolder;
    private File fastQFolder;
    private File reportsFolder;
    private File demultiplexStatsFile;

    public DragenFolderUtil(DragenConfig dragenConfig, IlluminaSequencingRun illuminaSequencingRun, String analysisKey) {
        this.dragenConfig = dragenConfig;
        this.illuminaSequencingRun = illuminaSequencingRun;
        this.analysisKey = analysisKey;

        File baseFolder = new File(dragenConfig.getDemultiplexOutputPath());
        File machineFolder = new File(baseFolder, illuminaSequencingRun.getMachineName());
        runFolder = new File(machineFolder, illuminaSequencingRun.getRunName());
        if (!runFolder.exists()) {
            // TODO Won't exist if we change the paths
            throw new RuntimeException("Run folder doesn't exists " + runFolder.getPath());
        }

        dragenFolder = new File(runFolder, "dragen");
        if (!dragenFolder.exists()) {
            if (!dragenFolder.mkdir()) {
                throw new RuntimeException("Failed to make dragen folder " + dragenFolder.getPath());
            }
        }

        analysisFolder = new File(dragenFolder, analysisKey);
        if (!analysisFolder.exists()) {
            if (!analysisFolder.mkdir()) {
                throw new RuntimeException("Failed to make analysis folder " + analysisFolder.getPath());
            }
        }

        // These folders are made by the demultiplex operation on the dragen
        fastQFolder = new File(analysisFolder, "fastq");
        replayJsonFile = new File(fastQFolder, "replay.json");
        reportsFolder = new File(fastQFolder, "Reports");
        demultiplexStatsFile = new File(reportsFolder, "Demultiplex_Stats.csv");
        fastQListFile = new File(reportsFolder, "fastq_list.csv");
    }

    public DragenConfig getDragenConfig() {
        return dragenConfig;
    }

    public IlluminaSequencingRun getIlluminaSequencingRun() {
        return illuminaSequencingRun;
    }

    public String getAnalysisKey() {
        return analysisKey;
    }

    public File getRunFolder() {
        return runFolder;
    }

    public File getDragenFolder() {
        return dragenFolder;
    }

    public File getAnalysisFolder() {
        return analysisFolder;
    }

    public File getFastQFolder() {
        return fastQFolder;
    }

    public File getReportsFolder() {
        return reportsFolder;
    }

    public File getDemultiplexStatsFile() {
        return demultiplexStatsFile;
    }

    public File getReplayJsonFile() {
        return replayJsonFile;
    }

    public File getFastQListFile() {
        return fastQListFile;
    }
}
