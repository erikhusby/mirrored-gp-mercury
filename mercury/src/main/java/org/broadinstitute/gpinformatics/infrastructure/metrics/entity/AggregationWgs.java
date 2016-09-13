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
}
