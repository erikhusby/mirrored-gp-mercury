package org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.DragenConfig;
import org.zeroturnaround.exec.ProcessResult;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

@Dependent
public class DragenInfoFetcher {

    private static final Log log = LogFactory.getLog(SlurmController.class);

    @Inject
    private ShellUtils shellUtils;

    @Inject
    private DragenConfig dragenConfig;

    public String getVersion(String node) {
        List<String> cmds = Arrays.asList("ssh", node.trim(), buildCommand("-V"));
        ProcessResult processResult = runProcess(cmds);
        if (processResult.getExitValue() != 0) {
            String output = processResult.hasOutput() ? processResult.getOutput().getString() : "";
            log.error("Failed to get Dragen info with exit code: " + processResult.getExitValue() + " " + output);
            return "unknown";
        }

        return processResult.getOutput().getString().split("\\s+")[2];
    }

    private ProcessResult runProcess(List<String> cmd) {
        try {
            return shellUtils.runSyncProcess(cmd);
        } catch (Exception e) {
            log.error("Error attempting to run sync process", e);
            throw new RuntimeException(e);
        }
    }

    private String buildCommand(String argument) {
        return String.format("%sdragen_info %s", dragenConfig.getDragenPath(), argument);
    }
}
