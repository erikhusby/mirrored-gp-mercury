package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;

/**
 * Entity that holds basic information of a running/queued process.
 */
@Entity
@Audited
public abstract class ProcessTask extends Task {

    private String commandLineArgument;

    private Long processId;

    private String taskName;

    private String partition;

    public ProcessTask(String partition) {
        this.partition = partition;
    }

    public ProcessTask() {
    }

    public String getCommandLineArgument() {
        return commandLineArgument;
    }

    public void setCommandLineArgument(String commandLineArgument) {
        this.commandLineArgument = commandLineArgument;
    }

    public Long getProcessId() {
        return processId;
    }

    public void setProcessId(Long processId) {
        this.processId = processId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String name) {
        this.taskName = name;
    }

    public String getPartition() {
        return partition;
    }

    public void setPartition(String partition) {
        this.partition = partition;
    }

    public boolean hasProlog() {
        return false;
    }

    public String getPrologFileName() {
        return null;
    }

    public boolean requiresDragenPrefix() {
        return true;
    }
}
