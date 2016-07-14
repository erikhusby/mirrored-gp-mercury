/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2016 by the
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
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Collection;


@SuppressWarnings("JpaObjectClassSignatureInspection")
@Entity
@Table(name = "PICARD_ANALYSIS", schema = "METRICS")
public class PicardAnalysis implements Serializable {
    private static final long serialVersionUID = -883492009027470354L;
    @Id
    @Column(name = "ID")
    private Long id;

    @Column(insertable = false, updatable = false, name = "FLOWCELL_BARCODE")
    private String flowcellBarcode;

    @Column(insertable = false, updatable = false, name = "LANE")
    private Long lane;

    @Column(insertable = false, updatable = false, name = "LIBRARY_NAME")
    private String libraryName;

    public PicardAnalysis() {
    }

    @OneToMany(mappedBy = "picardAnalysis")
    @BatchSize(size = 100)
    private Collection<PicardFingerprint> picardFingerprints;

    @ManyToOne(optional = false)
    @BatchSize(size = 100)
    @JoinColumns(
            {@JoinColumn(name = "FLOWCELL_BARCODE", referencedColumnName = "FLOWCELL_BARCODE"),
                    @JoinColumn(name = "LANE", referencedColumnName = "LANE"),
                    @JoinColumn(name = "LIBRARY_NAME", referencedColumnName = "LIBRARY_NAME")}
    )
    private AggregationReadGroup aggregationReadGroups;
}
