package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.TaskResult;

public interface Dragen {
    TaskResult fireProcess(String commandLine, Task task);
}
