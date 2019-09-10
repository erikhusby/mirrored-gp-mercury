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
@Table(schema = "MERCURYDW", name = "SEQ_DEMULTIPLEX_RUN_METRIC")
public class SequencingDemultiplexMetric {

    @Id
    private Long sequencingRunMetricId;

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
    @Column(name = "SAMPLE_ALIAS")
    private String sampleAlias;

    @CsvBindByPosition(position = 9)
    @Column(name = "NUM_PERFECT_READS")
    private long numberOfPerfectReads;

    @CsvBindByPosition(position = 10)
    @Column(name = "NUM_OF_READS")
    private long numberOfReads;

    @CsvBindByPosition(position = 11)
    @Column(name = "NUM_OF_PCT_IDX_READS")
    private long numberOfPerfectIndexReads;

    @CsvBindByPosition(position = 12)
    @Column(name = "NUM_OF_ONE_MISMATCH_IDX_READS")
    private long numberOfOneMismatchIndexReads;

    @CsvBindByPosition(position = 13)
    @Column(name = "NUM_OF_Q30_BASES_PF")
    private BigDecimal numberOfQ30BasesPF;

    @CsvBindByPosition(position = 14)
    @Column(name = "MEAN_QUALITY_SCORE_PF")
    private String meanQualityScorePF;

    public SequencingDemultiplexMetric(int lane, String sampleAlias) {
        this.lane = lane;
        this.sampleAlias = sampleAlias;
    }

    public SequencingDemultiplexMetric() {
    }

    public Date getRunDate() {
        return runDate;
    }

    public void setRunDate(Date runDate) {
        this.runDate = runDate;
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

    public void setFlowcell(String barcode) {
        this.flowcell = barcode;
    }

    public String getDragenVersion() {
        return dragenVersion;
    }

    public void setDragenVersion(String dragenVersion) {
        this.dragenVersion = dragenVersion;
    }

    public String getAnalysisNode() {
        return analysisNode;
    }

    public void setAnalysisNode(String analysisNode) {
        this.analysisNode = analysisNode;
    }

    public String getAnalysisName() {
        return analysisName;
    }

    public void setAnalysisName(String analysisName) {
        this.analysisName = analysisName;
    }

    public void setAnalysisVersion(int analysisVersion) {
        this.analysisVersion = analysisVersion;
    }

    public int getAnalysisVersion() {
        return analysisVersion;
    }

    public int getLane() {
        return lane;
    }

    public void setLane(int lane) {
        this.lane = lane;
    }

    public String getSampleAlias() {
        return sampleAlias;
    }

    public void setSampleAlias(String sampleAlias) {
        this.sampleAlias = sampleAlias;
    }

    public long getNumberOfReads() {
        return numberOfReads;
    }

    public void setNumberOfReads(long numberOfReads) {
        this.numberOfReads = numberOfReads;
    }

    public long getNumberOfPerfectReads() {
        return numberOfPerfectReads;
    }

    public void setNumberOfPerfectReads(long numberOfPerfectReads) {
        this.numberOfPerfectReads = numberOfPerfectReads;
    }

    public long getNumberOfOneMismatchIndexReads() {
        return numberOfOneMismatchIndexReads;
    }

    public void setNumberOfOneMismatchIndexReads(long numberOfOneMismatchIndexReads) {
        this.numberOfOneMismatchIndexReads = numberOfOneMismatchIndexReads;
    }

    public BigDecimal getNumberOfQ30BasesPF() {
        return numberOfQ30BasesPF;
    }

    public void setNumberOfQ30BasesPF(BigDecimal numberOfQ30BasesPF) {
        this.numberOfQ30BasesPF = numberOfQ30BasesPF;
    }

    public String getMeanQualityScorePF() {
        return meanQualityScorePF;
    }

    public void setMeanQualityScorePF(String meanQualityScorePF) {
        this.meanQualityScorePF = meanQualityScorePF;
    }

    public long getNumberOfPerfectIndexReads() {
        return numberOfPerfectIndexReads;
    }

    public void setNumberOfPerfectIndexReads(long numberOfPerfectIndexReads) {
        this.numberOfPerfectIndexReads = numberOfPerfectIndexReads;
    }
}
