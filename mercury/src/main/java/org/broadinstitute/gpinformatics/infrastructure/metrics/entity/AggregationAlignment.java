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
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "AGGREGATION_ALIGNMENT", schema = "METRICS")
public class AggregationAlignment implements Serializable {
    @Id
    @Column(name = "AGGREGATION_ID", nullable = false, insertable = false, updatable = false)
    private Integer aggregationId;
    @Id
    private String category;
    @Column(name = "PF_ALIGNED_BASES") private Long pfAlignedBases;
    @ManyToOne
    @JoinColumn(name = "AGGREGATION_ID", referencedColumnName = "ID", nullable = false,  updatable = false, insertable = false)
    private Aggregation aggregation;

    /**
     * Default constructor for JPA.
     */
    protected AggregationAlignment() {}

    public AggregationAlignment(Long pfAlignedBases, String category) {
        this.pfAlignedBases = pfAlignedBases;
        this.category = category;
    }

    public Long getPfAlignedBases() {
        return pfAlignedBases;
    }

    public String getCategory() {
        return category;
    }

    public Aggregation getAggregation() {
        return aggregation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AggregationAlignment)) {
            return false;
        }

        AggregationAlignment that = (AggregationAlignment) o;

        if (aggregationId != that.aggregationId) {
            return false;
        }
        if (category != null ? !category.equals(that.category) : that.category != null) {
            return false;
        }
        if (pfAlignedBases != null ? !pfAlignedBases.equals(that.pfAlignedBases) : that.pfAlignedBases != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = aggregationId;
        result = 31 * result + (category != null ? category.hashCode() : 0);
        result = 31 * result + (pfAlignedBases != null ? pfAlignedBases.hashCode() : 0);
        return result;
    }
}
