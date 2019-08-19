package org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler;

import com.opencsv.bean.CsvBindByPosition;

public class QueueInfo {

    @CsvBindByPosition(position = 8)
    private String jobId;

    @CsvBindByPosition(position = 9)
    private String name;

    @CsvBindByPosition(position = 20)
    private String user;

    @CsvBindByPosition(position = 29)
    private int nodes;

    @CsvBindByPosition(position = 41)
    private String partition;

    @CsvBindByPosition(position = 44)
    private String time;

    @CsvBindByPosition(position = 45)
    private String state;

    public QueueInfo() {
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getPartition() {
        return partition;
    }

    public void setPartition(String partition) {
        this.partition = partition;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getNodes() {
        return nodes;
    }

    public void setNodes(int nodes) {
        this.nodes = nodes;
    }
}
