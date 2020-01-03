package org.broadinstitute.gpinformatics.mercury.entity.analysis;

import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessObject;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * This lists the desired coverage for the product
 */
@Entity
@Audited
@Table(name = "COVERAGE_TYPE", schema = "mercury")
public class CoverageType implements BusinessObject {
    @Id
    @SequenceGenerator(name = "SEQ_COVERAGE_TYPE", schema = "mercury", sequenceName = "SEQ_COVERAGE_TYPE", allocationSize = 10)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_COVERAGE_TYPE")
    private Long coverageTypeId;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "MEAN_COVERAGE")
    private BigDecimal meanCoverage;

    @Column(name = "PERCENT_OF_TARGET_BASES")
    private BigDecimal percentTargetBases;

    @Column(name = "TARGET_COVERAGE")
    private BigDecimal targetCoverage;

    protected CoverageType() {
    }

    public CoverageType(@Nonnull String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getBusinessKey() {
        return name;
    }

    public BigDecimal getMeanCoverage() {
        return meanCoverage;
    }

    public void setMeanCoverage(BigDecimal meanCoverage) {
        this.meanCoverage = meanCoverage;
    }

    public BigDecimal getPercentTargetBases() {
        return percentTargetBases;
    }

    public void setPercentTargetBases(BigDecimal percentTargetBases) {
        this.percentTargetBases = percentTargetBases;
    }

    public BigDecimal getTargetCoverage() {
        return targetCoverage;
    }

    public void setTargetCoverage(BigDecimal targetCoverage) {
        this.targetCoverage = targetCoverage;
    }
}
