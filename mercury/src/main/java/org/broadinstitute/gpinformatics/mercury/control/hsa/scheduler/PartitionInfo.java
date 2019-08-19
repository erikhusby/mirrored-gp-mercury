package org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler;


import com.opencsv.bean.CsvBindByPosition;

public class PartitionInfo {

    @CsvBindByPosition(position = 33)
    private String name;

    @CsvBindByPosition(position = 0)
    private String available;

    @CsvBindByPosition(position = 23)
    private int nodes;

    @CsvBindByPosition(position = 15)
    private String state;

    @CsvBindByPosition(position = 31)
    private String nodeList;

    @CsvBindByPosition(position = 17)
    private String version;

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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
