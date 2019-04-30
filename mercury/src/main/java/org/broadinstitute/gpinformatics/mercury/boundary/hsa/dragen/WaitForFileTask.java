package org.broadinstitute.gpinformatics.mercury.boundary.hsa.dragen;

import org.broadinstitute.gpinformatics.mercury.boundary.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.boundary.hsa.state.TaskResult;

import java.io.File;

public class WaitForFileTask implements Task {
    private File file;

    public WaitForFileTask(File file) {
        this.file = file;
    }

    @Override
    public TaskResult fireEvent(DragenAppContext context) {
        while (true) {
            if (file.exists()) {
                return new TaskResult("", 0);
            } else {
                try {
                    Thread.sleep(10 * 1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
