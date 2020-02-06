package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Task that represents uploading a cram file.
 */
@Entity
@Audited
public class GsUtilTask extends ComputeTask {

    private static final int PARALLEL_THREAD_COUNT = 4;

    private static final String[] GSUTIL_CP_PARALLEL_LIMIT_PARAMETERS = {
            "-o", "GSUtil:parallel_process_count=1",
            "-o", "GSUtil:parallel_thread_count=" + PARALLEL_THREAD_COUNT,
            "-o", "GSUtil:parallel_composite_upload_threshold=150M"
    };

    private static final String GCLOUD_AUTH_SH = "gcloud-auth.sh";

    public GsUtilTask() {
        super();
    }

    /**
     * Create copy task from local srcFile to destPath. Assumes large files by default that need to be broken up
     * and uploaded in parallel threads.
     * @param srcFile: local file
     * @param destPath: path of bucket e.g. gs://broad-gplims-dev
     */
    public static GsUtilTask cp(File srcFile, final String destPath) {
        GsUtilTask task = new GsUtilTask();
        task.setTaskName("Cp_" + srcFile.getName());

        List<String> cmds = gsUtilParallel();
        cmds.add(String.format("cp %s %s", srcFile.getPath(), destPath));
        task.setCommandLineArgument(String.join(" ", cmds));
        return task;
    }

    private static List<String> gsUtilParallel() {
        List<String> cmds = new ArrayList<>();
        // TODO JW Fix path issue
        cmds.add("/broad/software/free/Linux/redhat_7_x86_64/pkgs/google-cloud-sdk/bin/gsutil");
        cmds.addAll(Arrays.asList(GSUTIL_CP_PARALLEL_LIMIT_PARAMETERS));
        return cmds;
    }

    @Override
    public boolean hasProlog() {
        return true;
    }

    @Override
    public String getPrologFileName() {
        return GCLOUD_AUTH_SH;
    }

    @Override
    public boolean isExclusive() {
        return false;
    }

    @Override
    public boolean hasCpuPerTaskLimit() {
        return true;
    }

    @Override
    public int getCpusPerTask() {
        return PARALLEL_THREAD_COUNT;
    }
}
