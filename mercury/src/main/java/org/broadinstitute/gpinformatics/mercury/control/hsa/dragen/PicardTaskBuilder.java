package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import java.io.File;

public class PicardTaskBuilder {
    private final StringBuilder commandBuilder;

    public PicardTaskBuilder() {
        this.commandBuilder = new StringBuilder("java -jar $PICARD ");
    }

    public PicardTaskBuilder tool(String toolName) {
        appendCommand(String.format(toolName));
        return this;
    }

    public PicardTaskBuilder inputFile(File inputFile) {
        appendCommand(String.format("INPUT=%s", inputFile.getPath()));
        return this;
    }

    public PicardTaskBuilder genotypeFile(File genotypeFile) {
        appendCommand(String.format("GENOTYPES=%s", genotypeFile.getPath()));
        return this;
    }

    public PicardTaskBuilder haplotypeMap(File haplotypeMap) {
        appendCommand(String.format("HAPLOTYPE_MAP=%s", haplotypeMap.getPath()));
        return this;
    }

    public PicardTaskBuilder outputPrefix(String outputPrefix) {
        appendCommand(String.format("OUTPUT=%s", outputPrefix));
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
