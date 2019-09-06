package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import java.io.File;

public class DragenTaskBuilder {

    public static final String OUTPUT_DIRECTORY = "output-directory";
    public static final String OUTPUT_FILE_PREFIX = "output-file-prefix";
    public static final String FASTQ_LIST = "fastq-list";
    public static final String FASTQ_LIST_SAMPLE_ID = "fastq-list-sample-id";
    public static final String VC_SAMPLE_NAME = "vc-sample-name";
    public static final String ENABLE_VARIANT_CALLER = "enable-variant-caller";
    public static final String REFERENCE = "-r";

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
        appendCommand(String.format("--" + OUTPUT_DIRECTORY + " %s", outputDirectory.getPath()));
        return this;
    }

    public DragenTaskBuilder sampleSheet(File sampleSheet) {
        appendCommand(String.format("--sample-sheet %s", sampleSheet.getPath()));
        return this;
    }

    public DragenTaskBuilder reference(File reference) {
        appendCommand(String.format("-f " + REFERENCE + " %s", reference.getPath()));
        return this;
    }

    public DragenTaskBuilder fastQList(File fastQList) {
        appendCommand(String.format("--" + FASTQ_LIST + " %s", fastQList.getPath()));
        return this;
    }

    public DragenTaskBuilder fastQSampleId(String fastQSampleId) {
        appendCommand(String.format("--" + FASTQ_LIST_SAMPLE_ID + " %s", fastQSampleId));
        return this;
    }

    public DragenTaskBuilder intermediateResultsDir(File intermediateResultsDir) {
        appendCommand(String.format("--intermediate-results-dir %s", intermediateResultsDir.getPath()));
        return this;
    }

    public DragenTaskBuilder outputFilePrefix(String outputFilePrefix) {
        appendCommand(String.format("--" + OUTPUT_FILE_PREFIX + " %s", outputFilePrefix));
        return this;
    }

    public DragenTaskBuilder vcSampleName(String vcSampleName) {
        appendCommand(String.format("--" + VC_SAMPLE_NAME + " %s", vcSampleName));
        return this;
    }

    public DragenTaskBuilder enableVariantCaller(boolean b) {
        appendCommand(String.format("--" + ENABLE_VARIANT_CALLER + " %b", b));
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

    public DragenTaskBuilder qcCrossContaminationVcf(File file) {
        appendCommand(String.format("--qc-cross-cont-vcf %s", file.getPath()));
        return this;
    }

    /**
     * To process all samples together in the same run, regardless of the RGSM value
     */
    public DragenTaskBuilder fastQListAllSamples(boolean b) {
        appendCommand(String.format("--" + FASTQ_LIST + "t-allsamples %b", b));
        return this;
    }

    /**
     * if true then dragen will read multiple files by the sample name given in the file name, which can be used
     * to combine samples that have been distributed across multiple BCL lanes or flow cells
     */
    public DragenTaskBuilder combineMultipleSamplesByName(boolean b) {
        appendCommand(String.format("combine-samples-by-name %b", b));
        return this;
    }

    public DragenTaskBuilder outputForm(String format) {
        appendCommand(String.format("--output-format=%s", format));
        return this;
    }

    public String build() {
        return this.commandBuilder.toString();
    }

    public static String parseCommandFromArgument(String commandFlag, String commandLine) {
        return parseCommandFromArgument(commandFlag, commandLine, false);
    }

    public static String parseCommandFromArgument(String commandFlag, String commandLine, boolean matchExact) {
        String[] split = commandLine.split("\\s");
        for (int i = 0; i < split.length; i++) {
            if (matchExact) {
                if (split[i].equals(commandFlag) && i + 1 != split.length) {
                    return split[i + 1];
                }
            } else {
                if (split[i].contains(commandFlag) && i + 1 != split.length) {
                    return split[i + 1];
                }
            }
        }
        return null;
    }

    private void appendCommand(String cmd) {
        this.commandBuilder.append(cmd);
        this.commandBuilder.append(" ");
    }
}
