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
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "AGGREGATION_READ_GROUP", schema = "METRICS")
public class AggregationReadGroup implements Serializable {
    private static final long serialVersionUID = 3261955479443339355L;
    @EmbeddedId
    private AggregationReadGroupPK aggregationReadGroupPK;

    @Column(name = "AGGREGATION_ID", nullable = false, insertable = false, updatable = false)
    private Integer aggregationId;

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

    @OneToOne
    @JoinColumns({
        @JoinColumn(name="FLOWCELL_BARCODE", referencedColumnName = "FLOWCELL_BARCODE", insertable = false, updatable = false),
        @JoinColumn(name = "LIBRARY_NAME", referencedColumnName = "LIBRARY_NAME", insertable = false, updatable = false),
        @JoinColumn(name = "LANE", referencedColumnName = "LANE", insertable = false, updatable = false),
    })
    private ReadGroupIndex readGroupIndex;

    public AggregationReadGroup() {
    }

    public AggregationReadGroup(String flowcellBarcode, long lane, String libraryName,
                                ReadGroupIndex readGroupIndex) {
        this.flowcellBarcode = flowcellBarcode;
        this.lane = lane;
        this.libraryName = libraryName;
        this.readGroupIndex = readGroupIndex;
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

    public ReadGroupIndex getReadGroupIndex() {
        return readGroupIndex;
    }

    public void setReadGroupIndex(ReadGroupIndex readGroupIndex) {
        this.readGroupIndex = readGroupIndex;
    }
}
