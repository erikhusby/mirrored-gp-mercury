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

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class AggregationReadGroupPK implements Serializable {
    private static final long serialVersionUID = -1792866235375615254L;
    @Column(name = "AGGREGATION_ID", nullable = false, insertable = false, updatable = false)
    private Integer aggregationId;

    @Column(name = "FLOWCELL_BARCODE", nullable = false, insertable = false, updatable = false)
    private String flowcellBarcode;

    @Column(name = "LANE", nullable = false, insertable = false, updatable = false)
    private Integer lane;

    @Column(name = "LIBRARY_NAME", nullable = false, insertable = false, updatable = false)
    private String libraryName;

    public Integer getAggregationId() {
        return aggregationId;
    }

    public String getFlowcellBarcode() {
        return flowcellBarcode;
    }

    public Integer getLane() {
        return lane;
    }

    public String getLibraryName() {
        return libraryName;
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || (!OrmUtil.proxySafeIsInstance(o, AggregationReadGroupPK.class))) {
            return false;
        }

        if (!(o instanceof AggregationReadGroupPK)) {
            return false;
        }

        AggregationReadGroupPK that = OrmUtil.proxySafeCast(o, AggregationReadGroupPK.class);

        return new EqualsBuilder()
                .append(getAggregationId(), that.getAggregationId())
                .append(getFlowcellBarcode(), that.getFlowcellBarcode())
                .append(getLane(), that.getLane())
                .append(getLibraryName(), that.getLibraryName())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getAggregationId())
                .append(getFlowcellBarcode())
                .append(getLane())
                .append(getLibraryName())
                .toHashCode();
    }
}
