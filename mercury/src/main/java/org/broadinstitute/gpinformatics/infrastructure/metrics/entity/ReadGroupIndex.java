/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2017 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.metrics.entity;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionLibraryDescriptor;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(schema = "PICARD", name = "READ_GROUP_INDEX")
public class ReadGroupIndex implements Serializable{
    private static final long serialVersionUID = -7596440076704597298L;

    @Id
    private Long id;

    @Column(name = "FLOWCELL_BARCODE", nullable = false, insertable = false, updatable = false)
    private String flowcellBarcode;

    @Column(name = "LANE", nullable = false, insertable = false, updatable = false)
    private long lane;

    @Column(name = "LIBRARY_NAME", nullable = false, insertable = false, updatable = false)
    private String libraryName;
    @Column(name = "LIBRARY_TYPE", nullable = false, insertable = false, updatable = false)
    private String libraryType;
    @Column(name = "ANALYSIS_TYPE", nullable = false, insertable = false, updatable = false)
    private String analysisType;
    @Column(name = "RESEARCH_PROJECT_ID")
    private String project;
    @Column(name = "SAMPLE_ALIAS")
    private String sampleAlias;
    @Column(name = "PRODUCT_ORDER_ID")
    private String productOrderId;

    public ReadGroupIndex() {
    }

    public ReadGroupIndex(Long id, String flowcellBarcode, long lane, String libraryName, String libraryType,
                          String analysisType, String project, String sampleAlias, String productOrderId) {
        this.id = id;
        this.flowcellBarcode = flowcellBarcode;
        this.lane = lane;
        this.libraryName = libraryName;
        this.libraryType = libraryType;
        this.analysisType = analysisType;
        this.project = project;
        this.sampleAlias = sampleAlias;
        this.productOrderId = productOrderId;
    }


    public SubmissionLibraryDescriptor getLibraryType() {
        if (libraryType.equals("HybridSelection")) {
            return SubmissionLibraryDescriptor.WHOLE_EXOME;
        }
        if (libraryType.equals("WholeGenomeShotgun")) {
            return SubmissionLibraryDescriptor.WHOLE_GENOME;
        }
        if (libraryType.equals("cDNAShotgunReadTwoSense") || libraryType.equals("cDNAShotgunStrandAgnostic")
            || analysisType.equals("cDNA")) {
            if (!analysisType.equals("AssemblyWithoutReference")) {
                return SubmissionLibraryDescriptor.RNA_SEQ;

            }
        }
        return null;
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

    public String getProject() {
        return project;
    }

    public String getSampleAlias() {
        return sampleAlias;
    }

    public String getProductOrderId() {
        return productOrderId;
    }

    public String getAnalysisType() {
        return analysisType;
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || (!OrmUtil.proxySafeIsInstance(o, ReadGroupIndex.class))) {
            return false;
        }

        if (!(o instanceof ReadGroupIndex)) {
            return false;
        }

        ReadGroupIndex that = OrmUtil.proxySafeCast(o, ReadGroupIndex.class);

        return new EqualsBuilder()
            .append(lane, that.lane)
            .append(id, that.id)
            .append(flowcellBarcode, that.flowcellBarcode)
            .append(libraryName, that.libraryName)
            .append(libraryType, that.libraryType)
            .append(analysisType, that.analysisType)
            .append(project, that.project)
            .append(sampleAlias, that.sampleAlias)
            .append(productOrderId, that.productOrderId)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(id)
            .append(flowcellBarcode)
            .append(lane)
            .append(libraryName)
            .append(libraryType)
            .append(analysisType)
            .append(project)
            .append(sampleAlias)
            .append(productOrderId)
            .toHashCode();
    }
}
