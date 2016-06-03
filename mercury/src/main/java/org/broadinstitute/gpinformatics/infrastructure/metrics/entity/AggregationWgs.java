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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "AGGREGATION_WGS", schema = "METRICS")
public class AggregationWgs {
    @Id
    @Column(name = "AGGREGATION_ID")
    private Integer aggregationId;

    @Column(name = "MAD_COVERAGE")
    private Double meanCoverage;

    public AggregationWgs() {
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

        if (o == null || (!OrmUtil.proxySafeIsInstance(o, AggregationWgs.class))) {
            return false;
        }

        if (!(o instanceof AggregationWgs)) {
            return false;
        }

        AggregationWgs that = OrmUtil.proxySafeCast(o, AggregationWgs.class);

        return new EqualsBuilder()
                .append(aggregationId, that.aggregationId)
                .append(getMeanCoverage(), that.getMeanCoverage())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(aggregationId)
                .append(getMeanCoverage())
                .toHashCode();
    }
}
