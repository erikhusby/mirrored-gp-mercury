package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.File;
import java.util.Objects;

@Entity
@Audited
public class AlignmentTask extends ProcessTask {

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

    /**
     * Syntax
     * dragen -f -r /staging/reference/<reference> \
     * --fastq-list /path/to/fastq_list.csv \
     * --fastq-list-sample-id <Sample Name RGSM in fastq_list.csv> \
     * --output-directory /path/to/outputdir/<Sample Name> \
     * --intermediate-results-dir \
     * --output-file-prefix <Sample Name> \
     * --vc-sample-name <Sample Name> \
     * --enable-variant-caller true \
     * --enable-duplicate-marking true \
     * --enable-map-align-output true
     */
    public AlignmentTask(File reference, File fastQList, String fastQSampleId,
                         File outputDirectory, File intermediateResultsDir, String outputFilePrefix, String vcSampleName) {
        super("dragen");
        this.reference = reference;
        this.fastQList = fastQList;
        this.fastQSampleId = fastQSampleId;
        this.outputDir = outputDirectory;
        this.intermediateResultsDir = intermediateResultsDir;
        this.outputFilePrefix = outputFilePrefix;
        this.vcSampleName = vcSampleName;

        Objects.requireNonNull(reference, "reference directory must not be null.");
        Objects.requireNonNull(fastQList, "fastQList must not be null.");
        Objects.requireNonNull(fastQSampleId, "Sample Id must not be null.");
        Objects.requireNonNull(outputDirectory, "outputDir must not be null.");
        Objects.requireNonNull(intermediateResultsDir, "intermediateResultsDir must not be null.");
        Objects.requireNonNull(outputFilePrefix, "outputFilePrefix must not be null.");
        Objects.requireNonNull(vcSampleName, "vcSampleName must not be null.");

        String dragenTaskBuilder = new DragenTaskBuilder().
                reference(reference).
                fastQList(fastQList).
                fastQSampleId(fastQSampleId).
                outputDirectory(outputDirectory).
                intermediateResultsDir(intermediateResultsDir).
                outputFilePrefix(outputFilePrefix).
                vcSampleName(vcSampleName).
                outputForm("cram").
                enableVariantCaller(true).
                enableDuplicateMarking(true).
                enableMapAlignOutput(true).
                build();

        setCommandLineArgument(dragenTaskBuilder);
    }

    public AlignmentTask() {
    }

    public File getReference() {
        if (reference == null) {
            reference = new File(DragenTaskBuilder.parseCommandFromArgument(
                    DragenTaskBuilder.REFERENCE, getCommandLineArgument(), true));
        }
        return reference;
    }

    public File getFastQList() {
        return fastQList;
    }

    public String getFastQSampleId() {
        if (fastQSampleId == null) {
            fastQSampleId = DragenTaskBuilder.parseCommandFromArgument(
                    DragenTaskBuilder.FASTQ_LIST_SAMPLE_ID, getCommandLineArgument());
        }
        return fastQSampleId;
    }

    public File getOutputDir() {
        if (outputDir == null) {
            outputDir = new File(DragenTaskBuilder.parseCommandFromArgument(
                    DragenTaskBuilder.OUTPUT_DIRECTORY, getCommandLineArgument()));
        }
        return outputDir;
    }

    public File getIntermediateResultsDir() {
        return intermediateResultsDir;
    }

    public String getOutputFilePrefix() {
        return outputFilePrefix;
    }

    public String getVcSampleName() {
        if (vcSampleName == null) {
            vcSampleName = DragenTaskBuilder.parseCommandFromArgument(
                    DragenTaskBuilder.VC_SAMPLE_NAME, getCommandLineArgument());
        }
        return vcSampleName;
    }

    public boolean isEnableVariantCaller() {
        String arg = DragenTaskBuilder.parseCommandFromArgument(
                DragenTaskBuilder.ENABLE_VARIANT_CALLER, getCommandLineArgument());
        if (arg != null) {
            return Boolean.valueOf(arg);
        }
        return false;
    }
}
