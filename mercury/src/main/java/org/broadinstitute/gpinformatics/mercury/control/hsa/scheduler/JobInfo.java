package org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler;

import com.opencsv.bean.CsvBindByName;

/**
 * Contains data from sacct -j {jobId} -p
 */
public class JobInfo {

    @CsvBindByName(column = "JobName")
    private String name;

    @CsvBindByName(column = "JobID")
    private String id;

    @CsvBindByName(column = "ExitCode")
    private String exitCode;

    @CsvBindByName(column = "State")
    private String state;

    @CsvBindByName(column = "Partition")
    private String partition;

    public JobInfo() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExitCode() {
        return exitCode;
    }

    public void setExitCode(String exitCode) {
        this.exitCode = exitCode;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPartition() {
        return partition;
    }

    public void setPartition(String partition) {
        this.partition = partition;
    }
}
