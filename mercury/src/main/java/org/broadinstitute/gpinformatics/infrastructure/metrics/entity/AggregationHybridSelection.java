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
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import javax.annotation.concurrent.Immutable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "AGGREGATION_HYBRID_SELECTION", schema = "METRICS")
@Immutable
public class AggregationHybridSelection implements Serializable {
    @Id
    @Column(name = "AGGREGATION_ID", nullable = false, insertable = false, updatable = false)
    private Integer aggregationId;
    @Column(name = "PCT_TARGET_BASES_20X")
    private Double pctTargetBases20X;

    public AggregationHybridSelection(Double pctTargetBases20X) {
        this.pctTargetBases20X = pctTargetBases20X;
    }

    public AggregationHybridSelection() {
    }

    public Double getPctTargetBases20X() {
        return pctTargetBases20X;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || (!OrmUtil.proxySafeIsInstance(o, AggregationHybridSelection.class))) {
            return false;
        }

        if (!(o instanceof AggregationHybridSelection)) {
            return false;
        }

        AggregationHybridSelection that = OrmUtil.proxySafeCast(o, AggregationHybridSelection.class);

        return new EqualsBuilder()
                .append(aggregationId, that.aggregationId)
                .append(getPctTargetBases20X(), that.getPctTargetBases20X())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(aggregationId)
                .append(getPctTargetBases20X())
                .toHashCode();
    }
}
