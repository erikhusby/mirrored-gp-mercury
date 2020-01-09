package org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import javax.enterprise.context.Dependent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Dependent
public class ShellUtils {

    private static final Log log = LogFactory.getLog(ShellUtils.class);

    public ProcessResult runSyncProcess(List<String> commands)
            throws InterruptedException, TimeoutException, IOException {
        log.debug(StringUtils.join(commands, " "));
        return new ProcessExecutor()
                .command(commands)
                .readOutput(true)
                .execute();
    }
}
