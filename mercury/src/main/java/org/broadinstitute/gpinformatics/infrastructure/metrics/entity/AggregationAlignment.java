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
    private int aggregationId;
    @Id
    private String category;
    @Column(name = "PF_READS_ALIGNED") private Integer pfReadsAligned;
    @Column(name = "PF_ALIGNED_BASES") private Integer pfAlignedBases;
    @ManyToOne
    @JoinColumn(name = "AGGREGATION_ID", referencedColumnName = "ID", nullable = false)
    private Aggregation aggregation;

    /**
     * Default constructor for JPA.
     */
    protected AggregationAlignment() {}

    public AggregationAlignment(Integer pfReadsAligned) {
        this.pfReadsAligned = pfReadsAligned;
    }

    public String getCategory() {
        return category;
    }

    public Integer getPfReadsAligned() {
        return pfReadsAligned;
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
        if (aggregation != null ? !aggregation.equals(that.aggregation) : that.aggregation != null) {
            return false;
        }
        if (category != null ? !category.equals(that.category) : that.category != null) {
            return false;
        }
        if (pfAlignedBases != null ? !pfAlignedBases.equals(that.pfAlignedBases) : that.pfAlignedBases != null) {
            return false;
        }
        if (pfReadsAligned != null ? !pfReadsAligned.equals(that.pfReadsAligned) : that.pfReadsAligned != null) {
            return false;
        }

        return true;
    }

    public Integer getPfAlignedBases() {
        return pfAlignedBases;
    }

    @Override
    public int hashCode() {
        int result = aggregationId;
        result = 31 * result + (category != null ? category.hashCode() : 0);
        result = 31 * result + (pfReadsAligned != null ? pfReadsAligned.hashCode() : 0);
        result = 31 * result + (pfAlignedBases != null ? pfAlignedBases.hashCode() : 0);
        result = 31 * result + (aggregation != null ? aggregation.hashCode() : 0);
        return result;
    }
}
