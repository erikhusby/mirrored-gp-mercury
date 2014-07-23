/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.metrics.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "PICARD_FINGERPRINT", schema = "METRICS")
public class PicardFingerprint implements Serializable {
    @Id
    private int picardAnalysisId;
    private Double lodExpectedSample;
    @OneToOne
    @JoinColumn(name = "PICARD_ANALYSIS_ID", referencedColumnName = "ID", nullable = false)
    private PicardAnalysis picardAnalysis;

    public PicardFingerprint() {
    }

    public PicardFingerprint(int picardAnalysisId,  Double lodExpectedSample) {
        this.picardAnalysisId = picardAnalysisId;
        this.lodExpectedSample = lodExpectedSample;
    }

    public void setLodExpectedSample(Double lodExpectedSample) {
        this.lodExpectedSample = lodExpectedSample;
    }

    public Double getLodExpectedSample() {
        return lodExpectedSample;
    }

    public PicardAnalysis getPicardAnalysis() {
        return picardAnalysis;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PicardFingerprint)) {
            return false;
        }

        PicardFingerprint that = (PicardFingerprint) o;

        if (picardAnalysisId != that.picardAnalysisId) {
            return false;
        }
        if (lodExpectedSample != null ? !lodExpectedSample.equals(that.lodExpectedSample) :
                that.lodExpectedSample != null) {
            return false;
        }
        if (picardAnalysis != null ? !picardAnalysis.equals(that.picardAnalysis) : that.picardAnalysis != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = picardAnalysisId;
        result = 31 * result + (lodExpectedSample != null ? lodExpectedSample.hashCode() : 0);
        result = 31 * result + (picardAnalysis != null ? picardAnalysis.hashCode() : 0);
        return result;
    }
}
