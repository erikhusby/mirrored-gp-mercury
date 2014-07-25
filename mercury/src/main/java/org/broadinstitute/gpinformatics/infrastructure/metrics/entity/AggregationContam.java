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
import java.io.Serializable;

@Entity
@Table(name = "AGGREGATION_CONTAM", schema = "METRICS")
public class AggregationContam implements Serializable {
    @Id
    @Column(name = "AGGREGATION_ID", nullable = false, insertable = false, updatable = false)
    private Integer aggregationId;
    @Column(name = "PCT_CONTAMINATION")
    private Double pctContamination;
    @OneToOne
    @JoinColumn(name = "AGGREGATION_ID", referencedColumnName = "ID", nullable = false, updatable = false, insertable = false)
    private Aggregation aggregation;

    public AggregationContam(Double pctContamination) {
        this.pctContamination = pctContamination;
    }

    public AggregationContam() {
    }

    public void setPctContamination(Double pctContamination) {
        this.pctContamination = pctContamination;
    }

    public Integer getAggregationId() {
        return aggregationId;
    }

    public Double getPctContamination() {
        return pctContamination;
    }

    public Aggregation getAggregation() {
        return aggregation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AggregationContam)) {
            return false;
        }

        AggregationContam that = (AggregationContam) o;

        if (aggregationId != null ? !aggregationId.equals(that.aggregationId) : that.aggregationId != null) {
            return false;
        }
        return !(pctContamination != null ? !pctContamination.equals(that.pctContamination) :
                that.pctContamination != null);

    }

    @Override
    public int hashCode() {
        int result = aggregationId != null ? aggregationId.hashCode() : 0;
        result = 31 * result + (pctContamination != null ? pctContamination.hashCode() : 0);
        return result;
    }
}
