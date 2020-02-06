package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import java.io.File;

public class Bcl2FastqTaskBuilder {
    private final StringBuilder commandBuilder;

    public static final String OUTPUT_DIRECTORY = "output-dir";
    public static final String BCL_INPUT_DIRECTORY = "input-dir";
    public static final String SAMPLE_SHEET = "sample-sheet";

    public Bcl2FastqTaskBuilder() {
        this.commandBuilder = new StringBuilder();
        // TODO
        appendCommand("/broad/software/free/Linux/redhat_7_x86_64/pkgs/bcl2fastq2_v2.20.0/bin/bcl2fastq");
    }

    public Bcl2FastqTaskBuilder bclInputDirectory(File runFolder) {
        appendCommand(String.format("--" + BCL_INPUT_DIRECTORY + " %s", runFolder.getPath()));
        return this;
    }

    public Bcl2FastqTaskBuilder outputDirectory(File outputDirectory) {
        appendCommand(String.format("--" + OUTPUT_DIRECTORY + " %s", outputDirectory.getPath()));
        return this;
    }

    public Bcl2FastqTaskBuilder sampleSheet(File sampleSheet) {
        appendCommand(String.format("--" + SAMPLE_SHEET + " %s", sampleSheet.getPath()));
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
