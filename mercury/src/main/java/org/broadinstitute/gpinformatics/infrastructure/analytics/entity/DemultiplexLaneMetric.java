package org.broadinstitute.gpinformatics.infrastructure.analytics.entity;

import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Entity for sequencing demultiplex metrics
 */
@Entity
@Table(schema = "MERCURYDW", name = "SEQ_DEMULTIPLEX_LANE_METRIC")
public class DemultiplexLaneMetric {
    @Id
    private Long sequencingLaneMetricId;

    @CsvBindByPosition(position = 0)
    @Column(name = "RUN_NAME")
    private String runName;

    @CsvBindByPosition(position = 1)
    @Column(name = "FLOWCELL")
    private String flowcell;

    @CsvDate(value = "yyyyMMddHHmmss")
    @CsvBindByPosition(position = 2)
    @Column(name = "RUN_DATE")
    private Date runDate;

    @CsvBindByPosition(position = 3)
    @Column(name = "DRAGEN_VERSION")
    private String dragenVersion;

    @CsvBindByPosition(position = 4)
    @Column(name = "ANALYSIS_VERSION")
    private int analysisVersion;

    @CsvBindByPosition(position = 5)
    @Column(name = "ANALYSIS_NAME")
    private String analysisName;

    @CsvBindByPosition(position = 6)
    @Column(name = "ANALYSIS_NODE")
    private String analysisNode;

    @CsvBindByPosition(position = 7)
    @Column(name = "LANE")
    private int lane;

    @CsvBindByPosition(position = 8)
    @Column(name = "ORPHAN_RATE")
    private BigDecimal orphanRate;

    public DemultiplexLaneMetric() {
    }

    public DemultiplexLaneMetric(int lane) {
        this.lane = lane;
    }

    public String getRunName() {
        return runName;
    }

    public void setRunName(String runName) {
        this.runName = runName;
    }

    public String getFlowcell() {
        return flowcell;
    }

    public void setFlowcell(String flowcell) {
        this.flowcell = flowcell;
    }

    public Date getRunDate() {
        return runDate;
    }

    public void setRunDate(Date runDate) {
        this.runDate = runDate;
    }

    public String getDragenVersion() {
        return dragenVersion;
    }

    public void setDragenVersion(String dragenVersion) {
        this.dragenVersion = dragenVersion;
    }

    public int getAnalysisVersion() {
        return analysisVersion;
    }

    public void setAnalysisVersion(int analysisVersion) {
        this.analysisVersion = analysisVersion;
    }

    public String getAnalysisName() {
        return analysisName;
    }

    public void setAnalysisName(String analysisName) {
        this.analysisName = analysisName;
    }

    public String getAnalysisNode() {
        return analysisNode;
    }

    public void setAnalysisNode(String analysisNode) {
        this.analysisNode = analysisNode;
    }

    public int getLane() {
        return lane;
    }

    public void setLane(int lane) {
        this.lane = lane;
    }

    public BigDecimal getOrphanRate() {
        return orphanRate;
    }

    public void setOrphanRate(BigDecimal orphanRate) {
        this.orphanRate = orphanRate;
    }
}
