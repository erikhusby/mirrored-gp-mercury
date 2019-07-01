package org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler;

import com.opencsv.bean.CsvBindByName;

public class QueueInfo {

    @CsvBindByName(column = "JOBID  ")
    private long jobId;

    @CsvBindByName(column = "PARTITION")
    private String partition;

    @CsvBindByName(column = "NAME")
    private String name;

    @CsvBindByName(column = "USER")
    private String user;

    @CsvBindByName(column = "ST")
    private String state;

    @CsvBindByName(column = "TIME")
    private String time;

    @CsvBindByName(column = "NODES")
    private int nodes;

    @CsvBindByName(column = "NODELIST(REASON)")
    private String nodeList;

    public QueueInfo() {
    }

    public long getJobId() {
        return jobId;
    }

    public void setJobId(long jobId) {
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

    public String getNodeList() {
        return nodeList;
    }

    public void setNodeList(String nodeList) {
        this.nodeList = nodeList;
    }
}
