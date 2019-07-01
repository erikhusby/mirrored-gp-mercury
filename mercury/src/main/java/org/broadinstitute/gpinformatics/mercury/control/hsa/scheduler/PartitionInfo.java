package org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler;


import com.opencsv.bean.CsvBindByName;

public class PartitionInfo {

    @CsvBindByName(column = "PARTITION")
    private String name;

    @CsvBindByName(column = "AVAIL")
    private String available;

    @CsvBindByName(column = "TIMELIMIT")
    private String timelimit;

    @CsvBindByName(column = "NODES")
    private int nodes;

    @CsvBindByName(column = "STATE")
    private String state;

    @CsvBindByName(column = "NODELIST")
    private String nodeList;

    public PartitionInfo() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvailable() {
        return available;
    }

    public void setAvailable(String available) {
        this.available = available;
    }

    public String getTimelimit() {
        return timelimit;
    }

    public void setTimelimit(String timelimit) {
        this.timelimit = timelimit;
    }

    public int getNodes() {
        return nodes;
    }

    public void setNodes(int nodes) {
        this.nodes = nodes;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getNodeList() {
        return nodeList;
    }

    public void setNodeList(String nodeList) {
        this.nodeList = nodeList;
    }
}
