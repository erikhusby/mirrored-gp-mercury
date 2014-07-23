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
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "PICARD_ANALYSIS", schema = "METRICS", catalog = "")
public class PicardAnalysis implements Serializable {
    @Id
    private int id;

    @Column(name="FLOWCELL_BARCODE")
    private String flowcellBarcode;
    @Column(name="LANE")
    private String lane;
    private String libraryName;

    public PicardAnalysis() {
    }

    public PicardAnalysis(int id, String flowcellBarcode, String lane,
                          String libraryName, PicardFingerprint picardFingerprint) {
        this.id = id;
        this.flowcellBarcode = flowcellBarcode;
        this.lane = lane;
        this.libraryName = libraryName;
        this.picardFingerprint = picardFingerprint;
    }

    @OneToOne(mappedBy = "picardAnalysis")
    private PicardFingerprint picardFingerprint;


    public String getFlowcellBarcode() {
        return flowcellBarcode;
    }

    public String getLane() {
        return lane;
    }

    public String getLibraryName() {
        return libraryName;
    }

    public PicardFingerprint getPicardFingerprint() {
        return picardFingerprint;
    }

    public void setPicardFingerprint(PicardFingerprint picardFingerprint) {
        this.picardFingerprint = picardFingerprint;
    }

    public void setLane(String lane) {
        this.lane = lane;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PicardAnalysis)) {
            return false;
        }

        PicardAnalysis that = (PicardAnalysis) o;

        if (id != that.id) {
            return false;
        }
        if (flowcellBarcode != null ? !flowcellBarcode.equals(that.flowcellBarcode) : that.flowcellBarcode != null) {
            return false;
        }
        if (lane != null ? !lane.equals(that.lane) : that.lane != null) {
            return false;
        }
        if (libraryName != null ? !libraryName.equals(that.libraryName) : that.libraryName != null) {
            return false;
        }
        if (picardFingerprint != null ? !picardFingerprint.equals(that.picardFingerprint) :
                that.picardFingerprint != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (flowcellBarcode != null ? flowcellBarcode.hashCode() : 0);
        result = 31 * result + (lane != null ? lane.hashCode() : 0);
        result = 31 * result + (libraryName != null ? libraryName.hashCode() : 0);
        result = 31 * result + (picardFingerprint != null ? picardFingerprint.hashCode() : 0);
        return result;
    }
}
