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

import javax.persistence.Basic;
import javax.persistence.Column;
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
    private String readGroup;
    private String sample;
    private Double llExpectedSample;
    private Double llRandomSample;
    private Double lodExpectedSample;
    private Integer haplotypesWithGenotypes;
    private Integer haplotypesConfidentlyChecked;
    @Basic
    @Column(name = "HAPLOTYPES_CONFIDENTLY_MATCHIN", nullable = true, insertable = true, updatable = true)
    private Integer haplotypesConfidentlyMatching;
    @OneToOne
    @JoinColumn(name = "PICARD_ANALYSIS_ID", referencedColumnName = "ID", nullable = false)
    private PicardAnalysis picardAnalysis;

    public PicardFingerprint() {
    }

    public PicardFingerprint(int picardAnalysisId, String readGroup, String sample, Double llExpectedSample,
                             Double llRandomSample, Double lodExpectedSample,
                             Integer haplotypesWithGenotypes, Integer haplotypesConfidentlyChecked,
                             Integer haplotypesConfidentlyMatching) {
        this.picardAnalysisId = picardAnalysisId;
        this.readGroup = readGroup;
        this.sample = sample;
        this.llExpectedSample = llExpectedSample;
        this.llRandomSample = llRandomSample;
        this.lodExpectedSample = lodExpectedSample;
        this.haplotypesWithGenotypes = haplotypesWithGenotypes;
        this.haplotypesConfidentlyChecked = haplotypesConfidentlyChecked;
        this.haplotypesConfidentlyMatching = haplotypesConfidentlyMatching;
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
        if (haplotypesConfidentlyChecked != null ?
                !haplotypesConfidentlyChecked.equals(that.haplotypesConfidentlyChecked) :
                that.haplotypesConfidentlyChecked != null) {
            return false;
        }
        if (haplotypesConfidentlyMatching != null ?
                !haplotypesConfidentlyMatching.equals(that.haplotypesConfidentlyMatching) :
                that.haplotypesConfidentlyMatching != null) {
            return false;
        }
        if (haplotypesWithGenotypes != null ? !haplotypesWithGenotypes.equals(that.haplotypesWithGenotypes) :
                that.haplotypesWithGenotypes != null) {
            return false;
        }
        if (llExpectedSample != null ? !llExpectedSample.equals(that.llExpectedSample) :
                that.llExpectedSample != null) {
            return false;
        }
        if (llRandomSample != null ? !llRandomSample.equals(that.llRandomSample) : that.llRandomSample != null) {
            return false;
        }
        if (lodExpectedSample != null ? !lodExpectedSample.equals(that.lodExpectedSample) :
                that.lodExpectedSample != null) {
            return false;
        }
        if (readGroup != null ? !readGroup.equals(that.readGroup) : that.readGroup != null) {
            return false;
        }
        if (sample != null ? !sample.equals(that.sample) : that.sample != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = picardAnalysisId;
        result = 31 * result + (readGroup != null ? readGroup.hashCode() : 0);
        result = 31 * result + (sample != null ? sample.hashCode() : 0);
        result = 31 * result + (llExpectedSample != null ? llExpectedSample.hashCode() : 0);
        result = 31 * result + (llRandomSample != null ? llRandomSample.hashCode() : 0);
        result = 31 * result + (lodExpectedSample != null ? lodExpectedSample.hashCode() : 0);
        result = 31 * result + (haplotypesWithGenotypes != null ? haplotypesWithGenotypes.hashCode() : 0);
        result = 31 * result + (haplotypesConfidentlyChecked != null ? haplotypesConfidentlyChecked.hashCode() : 0);
        result = 31 * result + (haplotypesConfidentlyMatching != null ? haplotypesConfidentlyMatching.hashCode() : 0);
        return result;
    }

    public String getReadGroup() {
        return readGroup;
    }

    public String getSample() {
        return sample;
    }

    public Double getLlExpectedSample() {
        return llExpectedSample;
    }

    public Double getLlRandomSample() {
        return llRandomSample;
    }

    public Double getLodExpectedSample() {
        return lodExpectedSample;
    }

    public Integer getHaplotypesWithGenotypes() {
        return haplotypesWithGenotypes;
    }

    public Integer getHaplotypesConfidentlyChecked() {
        return haplotypesConfidentlyChecked;
    }

    public Integer getHaplotypesConfidentlyMatching() {
        return haplotypesConfidentlyMatching;
    }

    public PicardAnalysis getPicardAnalysis() {
        return picardAnalysis;
    }
}
