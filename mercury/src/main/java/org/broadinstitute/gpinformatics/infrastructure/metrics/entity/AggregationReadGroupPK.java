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
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class AggregationReadGroupPK implements Serializable {
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AggregationReadGroupPK that = (AggregationReadGroupPK) o;

        if (!aggregationId.equals(that.aggregationId)) {
            return false;
        }
        if (flowcellBarcode != null ? !flowcellBarcode.equals(that.flowcellBarcode) : that.flowcellBarcode != null) {
            return false;
        }
        if (lane != null ? !lane.equals(that.lane) : that.lane != null) {
            return false;
        }
        return !(libraryName != null ? !libraryName.equals(that.libraryName) : that.libraryName != null);

    }

    @Override
    public int hashCode() {
        int result = aggregationId;
        result = 31 * result + (flowcellBarcode != null ? flowcellBarcode.hashCode() : 0);
        result = 31 * result + (lane != null ? lane.hashCode() : 0);
        result = 31 * result + (libraryName != null ? libraryName.hashCode() : 0);
        return result;
    }
}
