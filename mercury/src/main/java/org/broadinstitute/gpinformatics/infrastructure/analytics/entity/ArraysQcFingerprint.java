package org.broadinstitute.gpinformatics.infrastructure.analytics.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * Entity for array fingerprint metrics, ETL'd from cloud database by Analytics team.
 */
@Entity
@Table(schema = "ANALYTICS")
public class ArraysQcFingerprint {
    @Id
    private Long arraysQcId;
    private String readGroup;
    private String sample;
    private BigDecimal llExpectedSample;
    private BigDecimal llRandomSample;
    private BigDecimal lodExpectedSample;
    private Long haplotypesWithGenotypes;
    private Long haplotypesConfidentlyChecked;
    private Long haplotypesConfidentlyMatchin;
    private Long hetAsHom;
    private Long homAsHet;
    private Long homAsOtherHom;

    public Long getArraysQcId() {
        return arraysQcId;
    }

    public String getReadGroup() {
        return readGroup;
    }

    public String getSample() {
        return sample;
    }

    public BigDecimal getLlExpectedSample() {
        return llExpectedSample;
    }

    public BigDecimal getLlRandomSample() {
        return llRandomSample;
    }

    public BigDecimal getLodExpectedSample() {
        return lodExpectedSample;
    }

    public Long getHaplotypesWithGenotypes() {
        return haplotypesWithGenotypes;
    }

    public Long getHaplotypesConfidentlyChecked() {
        return haplotypesConfidentlyChecked;
    }

    public Long getHaplotypesConfidentlyMatchin() {
        return haplotypesConfidentlyMatchin;
    }

    public Long getHetAsHom() {
        return hetAsHom;
    }

    public Long getHomAsHet() {
        return homAsHet;
    }

    public Long getHomAsOtherHom() {
        return homAsOtherHom;
    }
}
