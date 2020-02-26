package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GsUtilTaskBuilder {

    private final StringBuilder commandBuilder;

    public GsUtilTaskBuilder() {
        this.commandBuilder = new StringBuilder();
        appendCommand("/broad/software/free/Linux/redhat_7_x86_64/pkgs/google-cloud-sdk/bin/gsutil");
    }

    public GsUtilTaskBuilder parallelProcessCount(int count) {
        appendCommand("-o");
        appendCommand("GSUtil:parallel_process_count=" + count);
        return this;
    }

    public GsUtilTaskBuilder parallelThreadCount(int count) {
        appendCommand("-o");
        appendCommand("GSUtil:parallel_thread_count=" + count);
        return this;
    }

    public GsUtilTaskBuilder parallelCompositeUploadThreshold(String size) {
        appendCommand("-o");
        appendCommand("GSUtil:parallel_composite_upload_threshold=" + size);
        return this;
    }

    public GsUtilTaskBuilder cp(File srcFile, String destPath) {
        appendCommand(String.format("cp %s %s", srcFile.getPath(), destPath));
        return this;
    }

    private void appendCommand(String cmd) {
        this.commandBuilder.append(cmd);
        this.commandBuilder.append(" ");
    }

    public String build() {
        return this.commandBuilder.toString();
    }
}
