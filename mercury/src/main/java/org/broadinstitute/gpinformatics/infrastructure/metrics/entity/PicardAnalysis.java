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
import java.util.Date;

@Entity
@Table(name = "PICARD_ANALYSIS", schema = "METRICS", catalog = "")
public class PicardAnalysis implements Serializable {
    @Id
    private int id;

    @Column(name="FLOWCELL_BARCODE")
    private String flowcellBarcode;
    @Column(name="LANE")
    private String lane;
    @Column(name="MOLECULAR_BARCODE_NAME")
    private String molecularBarcodeName;
    private String libraryName;
    private Date createdAt;
    private Date modifiedAt;
    private Date workflowStartDate;
    private Date workflowEndDate;
    private String runName;
    private String metricsType;

    public PicardAnalysis() {
    }

    public PicardAnalysis(int id, String flowcellBarcode, String lane, String molecularBarcodeName, String libraryName,
                          Date createdAt, Date modifiedAt, Date workflowStartDate, Date workflowEndDate,
                          String runName, String metricsType,
                          PicardFingerprint picardFingerprint) {
        this.id = id;
        this.flowcellBarcode = flowcellBarcode;
        this.lane = lane;
        this.molecularBarcodeName = molecularBarcodeName;
        this.libraryName = libraryName;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
        this.workflowStartDate = workflowStartDate;
        this.workflowEndDate = workflowEndDate;
        this.runName = runName;
        this.metricsType = metricsType;
        this.picardFingerprint = picardFingerprint;
    }

    @OneToOne(mappedBy = "picardAnalysis")
    private PicardFingerprint picardFingerprint;


    public String getMolecularBarcodeName() {
        return molecularBarcodeName;
    }

    public String getFlowcellBarcode() {
        return flowcellBarcode;
    }

    public String getLane() {
        return lane;
    }

    public String getLibraryName() {
        return libraryName;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getModifiedAt() {
        return modifiedAt;
    }

    public Date getWorkflowStartDate() {
        return workflowStartDate;
    }

    public Date getWorkflowEndDate() {
        return workflowEndDate;
    }

    public String getRunName() {
        return runName;
    }

    public String getMetricsType() {
        return metricsType;
    }

    public PicardFingerprint getPicardFingerprint() {
        return picardFingerprint;
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
        if (createdAt != null ? !createdAt.equals(that.createdAt) : that.createdAt != null) {
            return false;
        }
        if (metricsType != null ? !metricsType.equals(that.metricsType) : that.metricsType != null) {
            return false;
        }
        if (modifiedAt != null ? !modifiedAt.equals(that.modifiedAt) : that.modifiedAt != null) {
            return false;
        }
        if (molecularBarcodeName != null ? !molecularBarcodeName.equals(that.molecularBarcodeName) :
                that.molecularBarcodeName != null) {
            return false;
        }
        if (picardFingerprint != null ? !picardFingerprint.equals(that.picardFingerprint) :
                that.picardFingerprint != null) {
            return false;
        }
        if (runName != null ? !runName.equals(that.runName) : that.runName != null) {
            return false;
        }
        if (workflowEndDate != null ? !workflowEndDate.equals(that.workflowEndDate) : that.workflowEndDate != null) {
            return false;
        }
        if (workflowStartDate != null ? !workflowStartDate.equals(that.workflowStartDate) :
                that.workflowStartDate != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (molecularBarcodeName != null ? molecularBarcodeName.hashCode() : 0);
        result = 31 * result + (createdAt != null ? createdAt.hashCode() : 0);
        result = 31 * result + (modifiedAt != null ? modifiedAt.hashCode() : 0);
        result = 31 * result + (workflowStartDate != null ? workflowStartDate.hashCode() : 0);
        result = 31 * result + (workflowEndDate != null ? workflowEndDate.hashCode() : 0);
        result = 31 * result + (runName != null ? runName.hashCode() : 0);
        result = 31 * result + (metricsType != null ? metricsType.hashCode() : 0);
        result = 31 * result + (picardFingerprint != null ? picardFingerprint.hashCode() : 0);
        return result;
    }
}
