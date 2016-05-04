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

import javax.annotation.concurrent.Immutable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
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
        if (!(o instanceof AggregationHybridSelection)) {
            return false;
        }

        AggregationHybridSelection that = (AggregationHybridSelection) o;

        if (!aggregationId.equals(that.aggregationId)) {
            return false;
        }
        return !(pctTargetBases20X != null ? !pctTargetBases20X.equals(that.pctTargetBases20X) :
                that.pctTargetBases20X != null);

    }

    @Override
    public int hashCode() {
        int result = aggregationId;
        result = 31 * result + (pctTargetBases20X != null ? pctTargetBases20X.hashCode() : 0);
        return result;
    }
}
