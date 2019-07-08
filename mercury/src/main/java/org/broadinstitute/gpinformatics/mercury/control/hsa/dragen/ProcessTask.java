package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;

@Entity
@Audited
public class ProcessTask extends Task {

    private String commandLineArgument;

    private long processId;

    private String taskName;

    public String getCommandLineArgument() {
        return commandLineArgument;
    }

    public void setCommandLineArgument(String commandLineArgument) {
        this.commandLineArgument = commandLineArgument;
    }

    public long getProcessId() {
        return processId;
    }

    public void setProcessId(long processId) {
        this.processId = processId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String name) {
        this.taskName = name;
    }
}
