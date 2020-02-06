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

    /**
     * Job Allocation cannot share nodes. e.g. all dragen tasks must be exclusive per Illumina's documentation
     * @return - true if job allocation cannot share nodes.
     */
    public boolean isExclusive() {
        return true;
    }

    /**
     * @return true if job allocation should include cpus-per-task flag. Shouldn't be used for dragens as those
     * are set to exclusive and will thus use all of the cpus on a node.
     */
    public boolean hasCpuPerTaskLimit () {
        return false;
    }

    /**
     * Informs the slurm controller that ensuing jobs will require n-cpus number of processors per task.
     * @return number of cpus on a node to be allocated to job.
     */
    public int getCpusPerTask() {
        return -1;
    }
}
