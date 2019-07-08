package org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler;

import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import javax.enterprise.context.Dependent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Dependent
public class ShellUtils {

    public ProcessResult runSyncProcess(List<String> commands, ByteArrayOutputStream errStream)
            throws InterruptedException, TimeoutException, IOException {
        return new ProcessExecutor().redirectError(errStream)
                .command(commands)
                .readOutput(true)
                .execute();
    }
}
