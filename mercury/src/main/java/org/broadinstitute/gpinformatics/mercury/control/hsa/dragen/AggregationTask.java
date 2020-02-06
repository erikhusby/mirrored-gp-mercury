package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Transient;
import java.io.File;
import java.util.Objects;

@Entity
@Audited
public class AggregationTask extends ProcessTask {

    @Transient
    private File reference;

    @Transient
    private File fastQList;

    @Transient
    private String fastQSampleId;

    @Transient
    private File outputDir;

    @Transient
    private File intermediateResultsDir;

    @Transient
    private String outputFilePrefix;

    @Transient
    private String vcSampleName;

    @Transient
    private File qcContaminationFile;

    @Transient
    private File qcCoverageBedFile;

    public AggregationTask() {
    }

    public AggregationTask(File reference, File fastQList, String fastQSampleId, File outputDirectory,
                           File intermediateResultsDir, String outputFilePrefix, String vcSampleName,
                           File qcContaminationFile, File qcCoverageBedFile, File qcCoverageBed2File,
                           File qcCoverageBed3File, File configFile) {
        super("dragen");
        this.reference = reference;
        this.fastQList = fastQList;
        this.fastQSampleId = fastQSampleId;
        this.outputDir = outputDirectory;
        this.intermediateResultsDir = intermediateResultsDir;
        this.outputFilePrefix = outputFilePrefix;
        this.vcSampleName = vcSampleName;
        this.qcContaminationFile = qcContaminationFile;
        this.qcCoverageBedFile = qcCoverageBedFile;

        Objects.requireNonNull(reference, "reference directory must not be null.");
        Objects.requireNonNull(fastQList, "fastQList must not be null.");
        Objects.requireNonNull(fastQSampleId, "Sample Id must not be null.");
        Objects.requireNonNull(outputDirectory, "outputDir must not be null.");
        Objects.requireNonNull(intermediateResultsDir, "intermediateResultsDir must not be null.");
        Objects.requireNonNull(outputFilePrefix, "outputFilePrefix must not be null.");
        Objects.requireNonNull(vcSampleName, "vcSampleName must not be null.");
        Objects.requireNonNull(qcCoverageBedFile, "qcCoverageBedFile must not be null.");

        DragenTaskBuilder dragenTaskBuilder = new DragenTaskBuilder().
                reference(reference).
                fastQList(fastQList).
                fastQSampleId(fastQSampleId).
                outputDirectory(outputDirectory).
                intermediateResultsDir(intermediateResultsDir).
                outputFilePrefix(outputFilePrefix).
                vcSampleName(vcSampleName).
                configFile(configFile).
                qcCoverageRegion(qcCoverageBedFile).
                qcCoverageReports("cov_report");

        if (qcContaminationFile != null) {
            dragenTaskBuilder = dragenTaskBuilder.qcCrossContaminationVcf(qcContaminationFile);

        }
        if (qcCoverageBed2File != null) {
            dragenTaskBuilder = dragenTaskBuilder.
                    qcCoverageRegion2(qcCoverageBed2File).
                    qcCoverageReports2("cov_report");
        }

        if (qcCoverageBed3File != null) {
            dragenTaskBuilder = dragenTaskBuilder.
                    qcCoverageRegion3(qcCoverageBed3File).
                    qcCoverageReports3("cov_report");
        }

        String cmd = dragenTaskBuilder.build();

        setCommandLineArgument(cmd);
    }

    public AggregationTask(File reference, File fastQList, String fastQSampleId, File outputDirectory,
                           File intermediateResultsDir, String outputFilePrefix, String vcSampleName,
                           File qcContaminationFile, File qcCoverageBedFile, File qcCoverageBed2File,
                           File qcCoverageBed3File) {
        super("dragen");
        this.reference = reference;
        this.fastQList = fastQList;
        this.fastQSampleId = fastQSampleId;
        this.outputDir = outputDirectory;
        this.intermediateResultsDir = intermediateResultsDir;
        this.outputFilePrefix = outputFilePrefix;
        this.vcSampleName = vcSampleName;
        this.qcCoverageBedFile = qcCoverageBedFile;

        Objects.requireNonNull(reference, "reference directory must not be null.");
        Objects.requireNonNull(fastQList, "fastQList must not be null.");
        Objects.requireNonNull(fastQSampleId, "Sample Id must not be null.");
        Objects.requireNonNull(outputDirectory, "outputDir must not be null.");
        Objects.requireNonNull(intermediateResultsDir, "intermediateResultsDir must not be null.");
        Objects.requireNonNull(outputFilePrefix, "outputFilePrefix must not be null.");
        Objects.requireNonNull(vcSampleName, "vcSampleName must not be null.");
        Objects.requireNonNull(qcCoverageBedFile, "qcCoverageBedFile must not be null.");

        DragenTaskBuilder dragenTaskBuilder = new DragenTaskBuilder().
                reference(reference).
                fastQList(fastQList).
                fastQSampleId(fastQSampleId).
                outputDirectory(outputDirectory).
                intermediateResultsDir(intermediateResultsDir).
                outputFilePrefix(outputFilePrefix).
                vcSampleName(vcSampleName).
                enableVariantCaller(true).
                enableDuplicateMarking(true).
                enableMapAlignOutput(true).
                outputFormat("CRAM").
                qcCoverageRegion(qcCoverageBedFile).
                qcCoverageReports("cov_report");

        if (qcContaminationFile != null) {
            dragenTaskBuilder = dragenTaskBuilder.qcCrossContaminationVcf(qcContaminationFile);

        }

        if (qcCoverageBed2File != null) {
            dragenTaskBuilder = dragenTaskBuilder.
                    qcCoverageRegion2(qcCoverageBed2File).
                    qcCoverageReports2("cov_report");
        }

        if (qcCoverageBed3File != null) {
            dragenTaskBuilder = dragenTaskBuilder.
                    qcCoverageRegion3(qcCoverageBed3File).
                    qcCoverageReports3("cov_report");
        }

        String cmd = dragenTaskBuilder.build();

        setCommandLineArgument(cmd);
    }

    public File getFastQList() {
        if (fastQList == null) {
            fastQList = new File(DragenTaskBuilder.parseCommandFromArgument(
                    DragenTaskBuilder.FASTQ_LIST, getCommandLineArgument()));
        }
        return fastQList;
    }

    public File getOutputDir() {
        if (outputDir == null) {
            outputDir = new File(DragenTaskBuilder.parseCommandFromArgument(
                    DragenTaskBuilder.OUTPUT_DIRECTORY, getCommandLineArgument()));
        }
        return outputDir;
    }

    public String getFastQSampleId() {
        if (fastQSampleId == null) {
            fastQSampleId = DragenTaskBuilder.parseCommandFromArgument(
                    DragenTaskBuilder.FASTQ_LIST_SAMPLE_ID, getCommandLineArgument());
        }
        return fastQSampleId;
    }

    public String getOutputFilePrefix() {
        if (outputFilePrefix == null) {
            outputFilePrefix = DragenTaskBuilder.parseCommandFromArgument(
                    DragenTaskBuilder.OUTPUT_FILE_PREFIX, getCommandLineArgument());
        }
        return outputFilePrefix;
    }

    public File getQcContaminationFile() {
        if (qcContaminationFile == null) {
            String contamFilePath = DragenTaskBuilder.parseCommandFromArgument(
                    DragenTaskBuilder.QC_CROSS_CONT_VCF, getCommandLineArgument());
            if (contamFilePath == null) {
                return null;
            }
            qcContaminationFile = new File(contamFilePath);
        }
        return qcContaminationFile;
    }
}
