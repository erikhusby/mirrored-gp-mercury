package org.broadinstitute.gpinformatics.mercury.boundary.hsa.dragen;

import org.broadinstitute.gpinformatics.mercury.boundary.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.boundary.hsa.state.TaskResult;

public interface Dragen {
    TaskResult fireProcess(String commandLine, Task task);
}
