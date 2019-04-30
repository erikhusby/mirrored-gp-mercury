package org.broadinstitute.gpinformatics.mercury.boundary.hsa.dragen;

import java.io.File;

public class DragenTaskBuilder {

    private final StringBuilder commandBuilder;

    public DragenTaskBuilder() {
        this.commandBuilder = new StringBuilder();
        appendCommand("dragen");
    }

    public DragenTaskBuilder bclConversionOnly(boolean bclConversionOnly) {
        appendCommand(String.format("--bcl-conversion-only=%b", bclConversionOnly));
        return this;
    }

    public DragenTaskBuilder bclInputDirectory(File runFolder) {
        appendCommand(String.format("--bcl-input-directory %s", runFolder.getPath()));
        return this;
    }

    public DragenTaskBuilder outputDirectory(File outputDirectory) {
        appendCommand(String.format("--output-directory %s", outputDirectory.getPath()));
        return this;
    }

    public DragenTaskBuilder sampleSheet(File sampleSheet) {
        appendCommand(String.format("--sample-sheet %s", sampleSheet.getPath()));
        return this;
    }

    public DragenTaskBuilder reference(File reference) {
        appendCommand(String.format("-f -r %s", reference.getPath()));
        return this;
    }

    public DragenTaskBuilder fastQList(File fastQList) {
        appendCommand(String.format("--fastq-list %s", fastQList.getPath()));
        return this;
    }

    public DragenTaskBuilder fastQSampleId(String fastQSampleId) {
        appendCommand(String.format("--fastq-list-sample-id %s", fastQSampleId));
        return this;
    }

    public DragenTaskBuilder intermediateResultsDir(File intermediateResultsDir) {
        appendCommand(String.format("--intermediate-results-dir %s", intermediateResultsDir.getPath()));
        return this;
    }

    public DragenTaskBuilder outputFilePrefix(String outputFilePrefix) {
        appendCommand(String.format("--output-file-prefix %s", outputFilePrefix));
        return this;
    }

    public DragenTaskBuilder vcSampleName(String vcSampleName) {
        appendCommand(String.format("--vc-sample-name %s", vcSampleName));
        return this;
    }

    public DragenTaskBuilder enableVariantCaller(boolean b) {
        appendCommand(String.format("--enable-variant-caller %b", b));
        return this;
    }

    public DragenTaskBuilder enableDuplicateMarking(boolean b) {
        appendCommand(String.format("--enable-duplicate-marking %b", b));
        return this;
    }

    public DragenTaskBuilder enableMapAlignOutput(boolean b) {
        appendCommand(String.format("--enable-map-align-output %b", b));
        return this;
    }

    public String build() {
        return this.commandBuilder.toString();
    }

    private void appendCommand(String cmd) {
        this.commandBuilder.append(cmd);
        this.commandBuilder.append(" ");
    }
}
