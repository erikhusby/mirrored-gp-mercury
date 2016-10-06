package org.broadinstitute.gpinformatics.infrastructure.analytics.entity;

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
public class ArraysQcGtConcordance implements Serializable {
    @Id
    private Long arraysQcId;
    @Id
    private String variantType;
    private String truthSample;
    private BigDecimal hetSensitivity;
    private BigDecimal hetPpv;
    private BigDecimal hetSpecificity;
    private BigDecimal homvarSensitivity;
    private BigDecimal homvarPpv;
    private BigDecimal homvarSpecificity;
    private BigDecimal varSensitivity;
    private BigDecimal varPpv;
    private BigDecimal varSpecificity;
    private BigDecimal gtConcordance;
    private BigDecimal nonRefGtConcordance;

    public Long getArraysQcId() {
        return arraysQcId;
    }

    public String getVariantType() {
        return variantType;
    }

    public String getTruthSample() {
        return truthSample;
    }

    public BigDecimal getHetSensitivity() {
        return hetSensitivity;
    }

    public BigDecimal getHetPpv() {
        return hetPpv;
    }

    public BigDecimal getHetSpecificity() {
        return hetSpecificity;
    }

    public BigDecimal getHomvarSensitivity() {
        return homvarSensitivity;
    }

    public BigDecimal getHomvarPpv() {
        return homvarPpv;
    }

    public BigDecimal getHomvarSpecificity() {
        return homvarSpecificity;
    }

    public BigDecimal getVarSensitivity() {
        return varSensitivity;
    }

    public BigDecimal getVarPpv() {
        return varPpv;
    }

    public BigDecimal getVarSpecificity() {
        return varSpecificity;
    }

    public BigDecimal getGtConcordance() {
        return gtConcordance;
    }

    public BigDecimal getNonRefGtConcordance() {
        return nonRefGtConcordance;
    }
}
