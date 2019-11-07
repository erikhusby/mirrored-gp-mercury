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
                           File qcContaminationFile, File qcCoverageBedFile) {
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
        Objects.requireNonNull(qcContaminationFile, "qcContaminationFile must not be null.");
        Objects.requireNonNull(qcCoverageBedFile, "qcCoverageBedFile must not be null.");

        String dragenTaskBuilder = new DragenTaskBuilder().
                reference(reference).
                fastQList(fastQList).
                fastQSampleId(fastQSampleId).
                outputDirectory(outputDirectory).
                intermediateResultsDir(intermediateResultsDir).
                outputFilePrefix(outputFilePrefix).
                vcSampleName(vcSampleName).
                enableVariantCaller(true).
                enableDuplicateMarking(true).
                enableMapAlignOutput(false).
                qcCrossContaminationVcf(qcContaminationFile).
                qcCoverageRegion(qcCoverageBedFile).
                qcCoverageReports("cov_report").
                build();

        setCommandLineArgument(dragenTaskBuilder);
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
}
