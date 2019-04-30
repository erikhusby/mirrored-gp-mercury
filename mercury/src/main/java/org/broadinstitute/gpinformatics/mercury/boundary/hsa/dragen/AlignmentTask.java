package org.broadinstitute.gpinformatics.mercury.boundary.hsa.dragen;

import java.io.File;
import java.util.Objects;

public class AlignmentTask extends ProcessTask {

    private final File reference;
    private final File fastQList;
    private final String fastQSampleId;
    private final File outputDir;
    private final File intermediateResultsDir;
    private final String outputFilePrefix;
    private final String vcSampleName;

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
    public AlignmentTask(File reference, File fastQList, String fastQSampleId, File outputDirectory, File intermediateResultsDir,
                         String outputFilePrefix, String vcSampleName) {
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
                enableVariantCaller(true).
                enableDuplicateMarking(true).
                build();

        setCommandLineArgument(dragenTaskBuilder);
    }

    public File getReference() {
        return reference;
    }

    public File getFastQList() {
        return fastQList;
    }

    public String getFastQSampleId() {
        return fastQSampleId;
    }

    public File getOutputDir() {
        return outputDir;
    }

    public File getIntermediateResultsDir() {
        return intermediateResultsDir;
    }

    public String getOutputFilePrefix() {
        return outputFilePrefix;
    }

    public String getVcSampleName() {
        return vcSampleName;
    }
}
