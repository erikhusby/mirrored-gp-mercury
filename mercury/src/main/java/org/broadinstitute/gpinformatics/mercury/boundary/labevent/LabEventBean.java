package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

import org.broadinstitute.gpinformatics.mercury.boundary.Namespaces;

import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A JAX-RS DTO for lab events
 */
@XmlType(namespace = Namespaces.LAB_EVENT)
public class LabEventBean {
    private String eventType;
    private String station;
    private String operator;
    private Date eventDate;
    private String batchId;
    private List<LabVesselBean> sources = new ArrayList<>();
    private List<LabVesselBean> targets = new ArrayList<>();
    private List<TransferBean> transfers = new ArrayList<>();
    private List<ReagentBean> reagents = new ArrayList<>();
    private List<MetadataBean> metadatas = new ArrayList<>();

    public LabEventBean(String eventType, String station, String operator, Date eventDate) {
        this.eventType = eventType;
        this.station = station;
        this.operator = operator;
        this.eventDate = eventDate;
    }

    /** For JAXB */
    public LabEventBean() {
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getStation() {
        return station;
    }

    public void setStation(String station) {
        this.station = station;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    public List<LabVesselBean> getSources() {
        return sources;
    }

    public void setSources(List<LabVesselBean> sources) {
        this.sources = sources;
    }

    public List<LabVesselBean> getTargets() {
        return targets;
    }

    public void setTargets(List<LabVesselBean> targets) {
        this.targets = targets;
    }

    public List<TransferBean> getTransfers() {
        return transfers;
    }

    public void setTransfers(List<TransferBean> transfers) {
        this.transfers = transfers;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public List<ReagentBean> getReagents() {
        return reagents;
    }

    public void setReagents(List<ReagentBean> reagents) {
        this.reagents = reagents;
    }

    public List<MetadataBean> getMetadatas() {
        return metadatas;
    }

    public void setMetadatas(List<MetadataBean> metadatas) {
        this.metadatas = metadatas;
    }
}
