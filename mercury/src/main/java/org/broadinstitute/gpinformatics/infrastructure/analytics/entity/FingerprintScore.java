package org.broadinstitute.gpinformatics.infrastructure.analytics.entity;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(schema = "MERCURYDW", name = "FINGERPRINT_SCORE")
public class FingerprintScore {
    @SequenceGenerator(name = "SEQ_FP_SCORE", schema = "mercurydw", sequenceName = "SEQ_FP_SCORE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FP_SCORE")
    @Id
    private Long fingerprintScoreId;

    private String runName;

    private String flowcell;

    private int lane;

    private Date runDate;

    private String sampleAlias;

    private String analysisName;

    private BigDecimal lodScore;

    public FingerprintScore() {
    }

    public Long getFingerprintScoreId() {
        return fingerprintScoreId;
    }

    public void setFingerprintScoreId(Long fingerprintScoreId) {
        this.fingerprintScoreId = fingerprintScoreId;
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

    public int getLane() {
        return lane;
    }

    public void setLane(int lane) {
        this.lane = lane;
    }

    public Date getRunDate() {
        return runDate;
    }

    public void setRunDate(Date runDate) {
        this.runDate = runDate;
    }

    public String getSampleAlias() {
        return sampleAlias;
    }

    public void setSampleAlias(String sampleAlias) {
        this.sampleAlias = sampleAlias;
    }

    public String getAnalysisName() {
        return analysisName;
    }

    public void setAnalysisName(String analysisName) {
        this.analysisName = analysisName;
    }

    public BigDecimal getLodScore() {
        return lodScore;
    }

    public void setLodScore(BigDecimal lodScore) {
        this.lodScore = lodScore;
    }
}
