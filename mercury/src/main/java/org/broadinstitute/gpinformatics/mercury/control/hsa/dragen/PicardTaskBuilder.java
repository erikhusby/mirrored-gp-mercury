package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import java.io.File;

public class PicardTaskBuilder {
    public static final String INPUT = "INPUT=";
    public static final String OUTPUT = "OUTPUT=";
    public static final String REFERENCE_SEQUENCE = "REFERENCE_SEQUENCE=";
    public static final String GENOTYPES = "GENOTYPES=";
    public static final String HAPLOTYPE_MAP = "HAPLOTYPE_MAP=";

    private final StringBuilder commandBuilder;

    public PicardTaskBuilder() {
        this.commandBuilder = new StringBuilder("java -jar /seq/software/picard/current/bin/picard-private.jar ");
    }

    public PicardTaskBuilder tool(String toolName) {
        appendCommand(String.format(toolName));
        return this;
    }

    public PicardTaskBuilder inputFile(File inputFile) {
        appendCommand(String.format(INPUT + "%s", inputFile.getPath()));
        return this;
    }

    public PicardTaskBuilder genotypeFile(File genotypeFile) {
        appendCommand(String.format(GENOTYPES + "%s", genotypeFile.getPath()));
        return this;
    }

    public PicardTaskBuilder haplotypeMap(File haplotypeMap) {
        appendCommand(String.format(HAPLOTYPE_MAP + "%s", haplotypeMap.getPath()));
        return this;
    }

    public PicardTaskBuilder outputPrefix(String outputPrefix) {
        appendCommand(String.format(OUTPUT + "%s", outputPrefix));
        return this;
    }

    public PicardTaskBuilder referenceSequence(File refSeq) {
        appendCommand(String.format(REFERENCE_SEQUENCE + "%s", refSeq.getPath()));
        return this;
    }

    public String build() {
        return this.commandBuilder.toString();
    }

    private void appendCommand(String cmd) {
        this.commandBuilder.append(cmd);
        this.commandBuilder.append(" ");
    }

    public static String parseCommandFromArgument(String commandFlag, String commandLine) {
        String[] split = commandLine.split("\\s");
        for (int i = 0; i < split.length; i++) {
            if (split[i].contains(commandFlag)) {
                return split[i].replaceAll(commandFlag, "").trim();
            }
        }
        return null;
    }
}
