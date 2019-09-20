package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.ShellUtils;
import org.zeroturnaround.exec.ProcessResult;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

public abstract class AbstractMetricsTaskHandler extends AbstractTaskHandler {

    @Inject
    private ShellUtils shellUtils;

    protected ProcessResult uploadMetric(String ctlFilePath, File dataPath)
            throws IOException, TimeoutException, InterruptedException {
        return uploadMetric(ctlFilePath, dataPath, "load.log");
    }

    protected ProcessResult uploadMetric(String ctlFilePath, File dataPath, String loadLog)
            throws IOException, TimeoutException, InterruptedException {
        String ldruid = "mercurydw/seq_dev3@\"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=seqdev.broad.mit.edu)(PORT=1521)))(CONNECT_DATA=(SERVER=DEDICATED)(SID=seqdev3)))\"";
        List<String> cmds = Arrays.asList("sqlldr",
                String.format("control=%s", ctlFilePath),
                String.format("log=%s", loadLog),
                "bad=load.bad",
                String.format("data=%s", dataPath.getPath()),
                "discard=load.dsc",
                "direct=false",
                String.format("userId=%s", ldruid));
        return shellUtils.runSyncProcess(cmds);
    }

    public void setShellUtils(ShellUtils shellUtils) {
        this.shellUtils = shellUtils;
    }
}
