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

import org.hibernate.annotations.BatchSize;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "AGGREGATION_CONTAM", schema = "METRICS")
@BatchSize(size = 500)
public class AggregationContam implements Serializable {
    private static final long serialVersionUID = -4761300827083196585L;
    @Id
    @Column(name = "AGGREGATION_ID", nullable = false, insertable = false, updatable = false)
    private Integer aggregationId;
    @Column(name = "PCT_CONTAMINATION")
    private Double pctContamination;

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
}
