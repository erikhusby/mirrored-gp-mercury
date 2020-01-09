package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.DemultiplexStatsParser;
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.DragenReplayInfo;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.ShellUtils;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.zeroturnaround.exec.ProcessResult;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

public abstract class AbstractMetricsTaskHandler extends AbstractTaskHandler {

    private static final Log log = LogFactory.getLog(AbstractMetricsTaskHandler.class);

    @Inject
    private ShellUtils shellUtils;

    protected ProcessResult uploadMetric(String ctlFilePath, File dataPath)
            throws IOException, TimeoutException, InterruptedException {
        return uploadMetric(ctlFilePath, dataPath, "load.log");
    }

    protected ProcessResult uploadMetric(String ctlFilePath, File dataPath, String loadLog)
            throws IOException, TimeoutException, InterruptedException {
        String ldruid = "mercurydw/seq_dev3@\"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=seqdev.broad.mit.edu)(PORT=1521)))(CONNECT_DATA=(SERVER=DEDICATED)(SID=seqdev3)))\"";
        List<String> cmds = Arrays.asList("/Users/jowalsh/opt/oracle/sqlldr",
                String.format("control=%s", ctlFilePath),
                String.format("log=%s", loadLog),
                "bad=load.bad",
                String.format("data=%s", dataPath.getPath()),
                "discard=load.dsc",
                "direct=false",
                String.format("userId=%s", ldruid));
        return shellUtils.runSyncProcess(cmds);
    }

    public DragenReplayInfo parseReplay(File replayJsonFile, MessageCollection messageCollection) {
        try (InputStream inputStream = new FileInputStream(replayJsonFile)) {
            return new DemultiplexStatsParser().parseReplayInfo(inputStream, messageCollection);
        } catch (IOException e) {
            String errMsg = "Failed to read replay file " + replayJsonFile.getPath();
            log.error(errMsg, e);
            return null;
        }
    }

    public void setShellUtils(ShellUtils shellUtils) {
        this.shellUtils = shellUtils;
    }
}
