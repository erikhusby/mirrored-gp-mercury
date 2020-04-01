package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.broadinstitute.gpinformatics.infrastructure.deployment.DragenConfig;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.control.run.ConcordanceCalculator;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine.DEMUX_FOLDER_FORMAT;

/**
 * General file manipulation and folder creation of Dragen runs
 */
public class DragenFolderUtil {

    public static final String FAST_Q_FILE_NAME = "%s_S1_R%d_00%d.fastq.gz";
    public static final String FASTQ_LIST_CSV = "fastq_list.csv";
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
    private List<File> laneFolders = new ArrayList<>();

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
            throw new RuntimeException("Run folder doesn't exist " + runFolder.getPath()); // todo jmt
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
                    date = currDate;
                    analysisKey = file.getName();
                }
            } catch (ParseException e) {
                // ignore
            }
        }

        if (analysisKey == null) {
            throw new RuntimeException("Failed to find any demultiplex runs in " + runFolder.getPath());
        }

        analysisFolder = new File(dragenFolder, analysisKey);
        fastQFolder = new File(analysisFolder, "fastq");
        replayJsonFile = new File(fastQFolder, "replay.json");
        reportsFolder = new File(fastQFolder, "Reports");
        demultiplexStatsFile = new File(reportsFolder, "Demultiplex_Stats.csv");
        fastQListFile = new File(reportsFolder, FASTQ_LIST_CSV);

        for (IlluminaSequencingRunChamber runChamber: illuminaSequencingRun.getSequencingRunChambers()) {
            File laneFolder = new File(fastQFolder, String.valueOf(runChamber.getLaneNumber()));
            laneFolders.add(laneFolder);
        }
    }

    public DragenFolderUtil(DragenConfig dragenConfig, IlluminaSequencingRun illuminaSequencingRun, String analysisKey) {
        this.dragenConfig = dragenConfig;
        this.illuminaSequencingRun = illuminaSequencingRun;
        this.analysisKey = analysisKey;

        runFolder = new File(ConcordanceCalculator.convertFilePaths(illuminaSequencingRun.getRunDirectory()));
        if (!runFolder.exists()) {
            throw new RuntimeException("Run folder doesn't exist " + runFolder.getPath());
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

    public static File newSampleAggregation(DragenConfig dragenConfig, String sampleKey) {
        File aggregationFolder = new File(dragenConfig.getAggregationFilepath());
        File sampleFolder = new File(aggregationFolder, sampleKey);
        if (!sampleFolder.exists()) {
            sampleFolder.mkdir();
        }
        File aggregationVersion = new File(sampleFolder, DateUtils.getFileDateTime(new Date()));
        return aggregationVersion;
    }

    public static Optional<File> findMostRecentAggregation(DragenConfig dragenConfig, String sampleKey) {
        File aggregationFolder = new File(dragenConfig.getAggregationFilepath());
        if (!aggregationFolder.exists()) {
            return Optional.empty();
        }
        File sampleFolder = new File(aggregationFolder, sampleKey);
        if (!sampleFolder.exists()) {
            return Optional.empty();
        }
        List<File> analsisDirs = Arrays.asList(sampleFolder.listFiles(File::isDirectory));
        analsisDirs.remove(sampleFolder);
        Optional<File> recentAggFolder = analsisDirs.stream().max(Comparator.comparing(DateUtils::parseFileDateTime));
        if (recentAggFolder.isPresent()) {
            File sampleAggFolder = recentAggFolder.get();
            File cram = new File(sampleAggFolder, sampleKey + ".cram");
            if (cram.exists()) {
                return Optional.of(cram);
            }
        }
        return Optional.empty();
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
        return new File(fastQFolder, String.format("fastq_%s_list.csv", readGroup));
    }

    public File getReadOneFastQ(String sampleKey, int lane) {
        return new File(String.format(FAST_Q_FILE_NAME, sampleKey, 1, lane));
    }

    public File getReadTwoFastQ(String sampleKey, int lane) {
        return new File(String.format(FAST_Q_FILE_NAME, sampleKey, 2, lane));
    }

    public File getBamFile(String readGroup, int laneNumber) {
        File laneFolder = newlaneFolder(fastQFolder, laneNumber);
        return new File(laneFolder, readGroup + ".bam");
    }

    public File getVcfFile(String readGroup, int laneNumber) {
        File laneFolder = newlaneFolder(fastQFolder, laneNumber);
        return new File(laneFolder, readGroup + ".vcf.gz");
    }

    public static File fastQFileSampleLane(MercurySample mercurySample, int lane, File reportsFolder) {
        return new File(reportsFolder, mercurySample.getSampleKey() + "_" + lane + "_fastq_list.csv");
    }

    public File newlaneFolder(File fastQFolder, int laneNumber) {
        File laneFolder = new File(fastQFolder, String.valueOf(laneNumber));
        if (!laneFolder.exists()) {
            laneFolder.mkdir();
        }
        return laneFolder;
    }
}
