package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.deployment.DragenConfig;
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.DemultiplexStatsParser;
import org.broadinstitute.gpinformatics.mercury.control.hsa.metrics.DragenReplayInfo;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.ShellUtils;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.control.run.ConcordanceCalculator;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.zeroturnaround.exec.ProcessResult;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

public abstract class AbstractMetricsTaskHandler<T extends Task> extends AbstractTaskHandler<T> {

    private static final Log log = LogFactory.getLog(AbstractMetricsTaskHandler.class);

    @Inject
    private ShellUtils shellUtils;

    @Inject
    private DragenConfig dragenConfig;

    protected ProcessResult uploadMetric(String ctlFilePath, File dataPath)
            throws IOException, TimeoutException, InterruptedException {
        return uploadMetric(ctlFilePath, dataPath, "load.log");
    }

    protected ProcessResult uploadMetric(String ctlFilePath, File dataPath, String loadLog)
            throws IOException, TimeoutException, InterruptedException {
        // TODO JW set host from config
        String ldruid = "mercurydw/seq_dev3@\"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=seqdev.broad.mit.edu)(PORT=1521)))(CONNECT_DATA=(SERVER=DEDICATED)(SID=seqdev3)))\"";
        if (SystemUtils.IS_OS_WINDOWS) {
            ldruid = ldruid.replace("(", "\\(").replace(")", "\\)").replace("\"", "\\\"");
        }
        List<String> cmds = Arrays.asList(dragenConfig.getSqlldrPath(),
                String.format("control=%s", ConcordanceCalculator.convertFilePaths(ctlFilePath)),
                String.format("log=%s", ConcordanceCalculator.convertFilePaths(loadLog)),
                String.format("bad=%s/load.bad", dataPath.getParentFile().getPath()),
                String.format("data=%s", dataPath.getPath()),
                String.format("discard=%s/load.dsc", dataPath.getParentFile().getPath()),
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

    protected IlluminaSequencingRun findRunFromDirectory(File runDir, Set<IlluminaSequencingRunChamber> runChambers) {
        for (IlluminaSequencingRunChamber runChamber: runChambers) {
            String runDirectory = runChamber.getIlluminaSequencingRun().getRunDirectory();
            File seqRunDir = new File(runDirectory);
            if (seqRunDir.getName().equals(runDir.getName())) {
                return runChamber.getIlluminaSequencingRun();
            }
        }
        return null;
    }

    protected String getCtlFilePath(String ctlFilename) {
        return String.format("%s/%s", dragenConfig.getCtlFolder(), ctlFilename);
    }

    protected String getLogPath(String filename) {
        return String.format("%s/log/%s", dragenConfig.getCtlFolder(), filename);
    }

    public void setShellUtils(ShellUtils shellUtils) {
        this.shellUtils = shellUtils;
    }

    public void setDragenConfig(DragenConfig dragenConfig) {
        this.dragenConfig = dragenConfig;
    }
}
