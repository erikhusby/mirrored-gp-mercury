package org.broadinstitute.gpinformatics.infrastructure.analytics.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Entity for array concordance metrics, ETL'd from cloud database by Analytics team.
 */
@Entity
@Table(schema = "ANALYTICS")
public class ArraysQcContamination implements Serializable {
    @Id
    private Long arraysQcId;

    @Column(name = "ID")
    private Long contaminationId;

    private BigDecimal pctMix;
    private BigDecimal llk;
    private BigDecimal llk0;

    public Long getArraysQcId() {
        return arraysQcId;
    }

    public Long getContaminationId() {
        return contaminationId;
    }

    public BigDecimal getPctMix() {
        return pctMix;
    }

    public void setPctMix(BigDecimal pctMix) {
        this.pctMix = pctMix;
    }

    public BigDecimal getLlk() {
        return llk;
    }

    public BigDecimal getLlk0() {
        return llk0;
    }
}
