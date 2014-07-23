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
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "AGGREGATION_READ_GROUP", schema = "METRICS")
public class AggregationReadGroup implements Serializable {
    @EmbeddedId
    private AggregationReadGroupPK aggregationReadGroupPK;

    @Column(name = "AGGREGATION_ID", nullable = false, insertable = false, updatable = false)
    private int aggregationId;

    @Column(name = "FLOWCELL_BARCODE", nullable = false, insertable = false, updatable = false)
    private String flowcellBarcode;

    @Column(name = "LANE", nullable = false, insertable = false, updatable = false)
    private long lane;

    @Column(name = "LIBRARY_NAME", nullable = false, insertable = false, updatable = false)
    private String libraryName;


    @ManyToOne
    @JoinColumn(name = "AGGREGATION_ID", referencedColumnName = "ID", nullable = false, insertable = false,
            updatable = false)
    private Aggregation aggregation;

    public AggregationReadGroup() {
    }

    public AggregationReadGroup(int aggregationId, String flowcellBarcode,
            long lane, String libraryName, String molecularBarcodeName, boolean pairedEnd,
            List<PicardAnalysis> picardAnalysis) {
        this.aggregationId = aggregationId;
        this.flowcellBarcode = flowcellBarcode;
        this.lane = lane;
        this.libraryName = libraryName;
        this.picardAnalysis = picardAnalysis;
    }

    @Transient
    public List<PicardAnalysis> picardAnalysis=new ArrayList<>();

    public List<PicardAnalysis> getPicardAnalysis() {
        return picardAnalysis;
    }

    public void setPicardAnalysis(List<PicardAnalysis> picardAnalysis) {
        this.picardAnalysis = picardAnalysis;
    }

    public String getFlowcellBarcode() {
        return flowcellBarcode;
    }

    public long getLane() {
        return lane;
    }

    public String getLibraryName() {
        return libraryName;
    }

    public Aggregation getAggregation() {
        return aggregation;
    }

    public void setLane(long lane) {
        this.lane = lane;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AggregationReadGroup)) {
            return false;
        }

        AggregationReadGroup that = (AggregationReadGroup) o;

        if (aggregationId != that.aggregationId) {
            return false;
        }
        if (lane != that.lane) {
            return false;
        }
        if (aggregation != null ? !aggregation.equals(that.aggregation) : that.aggregation != null) {
            return false;
        }
        if (aggregationReadGroupPK != null ? !aggregationReadGroupPK.equals(that.aggregationReadGroupPK) :
                that.aggregationReadGroupPK != null) {
            return false;
        }
        if (flowcellBarcode != null ? !flowcellBarcode.equals(that.flowcellBarcode) : that.flowcellBarcode != null) {
            return false;
        }
        if (libraryName != null ? !libraryName.equals(that.libraryName) : that.libraryName != null) {
            return false;
        }
        if (picardAnalysis != null ? !picardAnalysis.equals(that.picardAnalysis) : that.picardAnalysis != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = aggregationReadGroupPK != null ? aggregationReadGroupPK.hashCode() : 0;
        result = 31 * result + aggregationId;
        result = 31 * result + (flowcellBarcode != null ? flowcellBarcode.hashCode() : 0);
        result = 31 * result + (int) (lane ^ (lane >>> 32));
        result = 31 * result + (libraryName != null ? libraryName.hashCode() : 0);
        result = 31 * result + (aggregation != null ? aggregation.hashCode() : 0);
        result = 31 * result + (picardAnalysis != null ? picardAnalysis.hashCode() : 0);
        return result;
    }
}
