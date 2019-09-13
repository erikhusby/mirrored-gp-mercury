package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.broadinstitute.gpinformatics.infrastructure.deployment.DragenConfig;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;

import java.io.File;
import java.text.ParseException;
import java.util.Date;

import static org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine.DEMUX_FOLDER_FORMAT;

public class DragenFolderUtil {

    public static final String FAST_Q_FILE_NAME = "%s_S1_R%d_00%d.fastq.gz";
    private final DragenConfig dragenConfig;
    private final IlluminaSequencingRun illuminaSequencingRun;
    private File replayJsonFile;
    private File fastQListFile;
    private String analysisKey;

    private File runFolder;
    private File dragenFolder;
    private File analysisFolder;
    private File fastQFolder;
    private File reportsFolder;
    private File demultiplexStatsFile;

    /**
     * Initializes as the most recent analysis folder found in root run folder
     */
    public DragenFolderUtil(DragenConfig dragenConfig, IlluminaSequencingRun illuminaSequencingRun) {
        this.dragenConfig = dragenConfig;
        this.illuminaSequencingRun = illuminaSequencingRun;

        File baseFolder = new File(dragenConfig.getDemultiplexOutputPath());
        File machineFolder = new File(baseFolder, illuminaSequencingRun.getMachineName());
        runFolder = new File(machineFolder, illuminaSequencingRun.getRunName());
        if (!runFolder.exists()) {
            throw new RuntimeException("Run folder doesn't exists " + runFolder.getPath());
        }

        dragenFolder = new File(runFolder, "dragen");
        if (!dragenFolder.exists()) {
            if (!dragenFolder.mkdir()) {
                throw new RuntimeException("Failed to make dragen folder " + dragenFolder.getPath());
            }
        }

        File[] analysisDirectories = dragenFolder.listFiles(File::isDirectory);
        if (analysisDirectories == null || analysisDirectories.length == 0) {
            throw new RuntimeException("No demultiplexes found in " + dragenFolder.getPath());
        }

        Date date = new Date(Long.MIN_VALUE);
        for (File file: analysisDirectories) {
            try {
                Date currDate = DEMUX_FOLDER_FORMAT.parse(file.getName());
                if (currDate.compareTo(date) > 0) {
                    analysisKey = file.getName();
                }
            } catch (ParseException e) {
                // ignore
            }
        }

        if (analysisKey == null) {
            throw new RuntimeException("Failed to find any demultiplex runs in " + runFolder.getPath());
        }

        fastQFolder = new File(analysisFolder, "fastq");
        replayJsonFile = new File(fastQFolder, "replay.json");
        reportsFolder = new File(fastQFolder, "Reports");
        demultiplexStatsFile = new File(reportsFolder, "Demultiplex_Stats.csv");
        fastQListFile = new File(reportsFolder, "fastq_list.csv");
    }

    public DragenFolderUtil(DragenConfig dragenConfig, IlluminaSequencingRun illuminaSequencingRun, String analysisKey) {
        this.dragenConfig = dragenConfig;
        this.illuminaSequencingRun = illuminaSequencingRun;
        this.analysisKey = analysisKey;

        File baseFolder = new File(dragenConfig.getDemultiplexOutputPath());
        File machineFolder = new File(baseFolder, illuminaSequencingRun.getMachineName());
        runFolder = new File(machineFolder, illuminaSequencingRun.getRunName());
        if (!runFolder.exists()) {
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

    public File getFastQReadGroupFile(String readGroup) {
        return new File(reportsFolder, String.format("fastq_%s_list.csv", readGroup));
    }

    public File getReadOneFastQ(String sampleKey, int lane) {
        return new File(String.format(FAST_Q_FILE_NAME, sampleKey, 1, lane));
    }

    public File getReadTwoFastQ(String sampleKey, int lane) {
        return new File(String.format(FAST_Q_FILE_NAME, sampleKey, 2, lane));
    }
}
