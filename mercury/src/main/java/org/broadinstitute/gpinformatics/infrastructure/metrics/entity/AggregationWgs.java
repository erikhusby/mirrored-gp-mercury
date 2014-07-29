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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "AGGREGATION_WGS", schema = "METRICS")
public class AggregationWgs {
    @Id
    @Column(name = "AGGREGATION_ID")
    private Integer aggregationId;

    @OneToOne
    @JoinColumn(name = "AGGREGATION_ID", referencedColumnName = "ID", nullable = false, updatable = false, insertable = false)
    private Aggregation aggregation;

    @Column(name = "MAD_COVERAGE")
    private Double meanCoverage;

    public AggregationWgs() {
    }

    public Aggregation getAggregation() {
        return aggregation;
    }

    public void setAggregation(Aggregation aggregation) {
        this.aggregation = aggregation;
    }

    public Integer getAggregationId() {
        return aggregationId;
    }

    public AggregationWgs(Double meanCoverage) {
        this.meanCoverage = meanCoverage;
    }

    public Double getMeanCoverage() {
        return meanCoverage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AggregationWgs that = (AggregationWgs) o;

        if (meanCoverage != null ? !meanCoverage.equals(that.meanCoverage) : that.meanCoverage != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return meanCoverage != null ? meanCoverage.hashCode() : 0;
    }
}
