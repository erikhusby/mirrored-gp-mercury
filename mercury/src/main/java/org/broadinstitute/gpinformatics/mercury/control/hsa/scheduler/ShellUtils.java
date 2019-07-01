package org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler;

import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class ShellUtils {

    public static ProcessResult runSyncProcess(String[] commands, ByteArrayOutputStream errStream)
            throws InterruptedException, TimeoutException, IOException {
        return new ProcessExecutor().redirectError(errStream)
                .command(commands)
                .readOutput(true)
                .execute();
    }
}
