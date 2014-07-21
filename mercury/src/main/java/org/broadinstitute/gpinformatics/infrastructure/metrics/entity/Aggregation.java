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

import org.broadinstitute.gpinformatics.infrastructure.metrics.LevelOfDetection;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "AGGREGATION", schema = "METRICS")
public class Aggregation {
    public static final String DATA_TYPE_EXOME = "Exome";
    public static final String DATA_TYPE_RNA = "RNA";
    public static final String DATA_TYPE_NA = "N/A";

    @Id
    private int id;
    private String project;
    private String sample;
    private String library;
    private int version;
    private Date createdAt;
    private Date modifiedAt;
    private Long isLatest;
    private int readGroupCount;
    private String aggregationType;
    private Date workflowStartDate;
    private Date workflowEndDate;
    private String dataType;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "aggregation")
    private Collection<AggregationAlignment> aggregationAlignments = new ArrayList<>();
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "aggregation")
    private AggregationContam aggregationContam;
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "aggregation")
    private AggregationHybridSelection aggregationHybridSelection;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "aggregation")
    private Collection<AggregationReadGroup> aggregationReadGroups = new ArrayList<>();
    @OneToOne(mappedBy = "aggregationByAggregationId")
    private AggregationWgs aggregationWgs;

    public Aggregation(String dataType) {
        this.dataType = dataType;
    }

    public Aggregation() {
    }

    public Double getQualityMetric() {
        switch (dataType) {
        case DATA_TYPE_EXOME:
            return aggregationHybridSelection.getPctTargetBases20X();
        case DATA_TYPE_RNA:
            int totalReadsAlignedInPairs = 0;
            for (AggregationAlignment aggregationAlignment : aggregationAlignments) {
                totalReadsAlignedInPairs += aggregationAlignment.getPfReadsAligned();
            }
            return (double) totalReadsAlignedInPairs;
        case DATA_TYPE_NA:
            if (aggregationWgs.getMeanCoverage()!=0){
                return aggregationWgs.getMeanCoverage();
            }
        default:
            return null;

        }
    }

    public String getQualityMetricString() {
        if (dataType==null){
            return null;
        }
        switch (dataType) {
        case DATA_TYPE_EXOME:
            return convertToPercent(getQualityMetric());
        case DATA_TYPE_RNA:
            return MessageFormat.format("{0,number,#}", getQualityMetric());
        case DATA_TYPE_NA:
            return "N/A";
        }
        return null;

    }

    protected String convertToPercent(double decimalValue) {
        return MessageFormat.format("{0,number,#.##%}", decimalValue);
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getSample() {
        return sample;
    }

    public void setSample(String sample) {
        this.sample = sample;
    }

    public String getLibrary() {
        return library;
    }

    public int getVersion() {
        return version;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getModifiedAt() {
        return modifiedAt;
    }

    public Long getIsLatest() {
        return isLatest;
    }

    public int getReadGroupCount() {
        return readGroupCount;
    }

    public void setReadGroupCount(int readGroupCount) {
        this.readGroupCount = readGroupCount;
    }

    public String getAggregationType() {
        return aggregationType;
    }

    public Date getWorkflowStartDate() {
        return workflowStartDate;
    }

    public Date getWorkflowEndDate() {
        return workflowEndDate;
    }

    public void setWorkflowEndDate(Date workflowEndDate) {
        this.workflowEndDate = workflowEndDate;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public Collection<AggregationAlignment> getAggregationAlignments() {
        return aggregationAlignments;
    }

    public void setAggregationAlignments(List<AggregationAlignment> aggregationAlignments) {
        this.aggregationAlignments = aggregationAlignments;
    }

    public AggregationContam getAggregationContam() {
        return aggregationContam;
    }

    public void setAggregationContam(AggregationContam aggregationContam) {
        this.aggregationContam = aggregationContam;
    }

    public String getContaminationString(){
        return convertToPercent(aggregationContam.getPctContamination());
    }

    public AggregationHybridSelection getAggregationHybridSelection() {
        return aggregationHybridSelection;
    }

    public void setAggregationHybridSelection(AggregationHybridSelection aggregationHybridSelection) {
        this.aggregationHybridSelection = aggregationHybridSelection;
    }

    public Collection<AggregationReadGroup> getAggregationReadGroups() {
        return aggregationReadGroups;
    }

    public void setAggregationReadGroups(Collection<AggregationReadGroup> aggregationReadGroups) {
        this.aggregationReadGroups = aggregationReadGroups;
    }


    public AggregationWgs getAggregationWgs() {
        return aggregationWgs;
    }

    public void setAggregationWgs(AggregationWgs aggregationWgsById) {
        this.aggregationWgs = aggregationWgsById;
    }

    @Transient
    public LevelOfDetection getLevelOfDetection() {
        return LevelOfDetection.calculate(getAggregationReadGroups());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Aggregation that = (Aggregation) o;

        if (id != that.id) {
            return false;
        }
        if (version != that.version) {
            return false;
        }
        if (aggregationType != null ? !aggregationType.equals(that.aggregationType) : that.aggregationType != null) {
            return false;
        }
        if (createdAt != null ? !createdAt.equals(that.createdAt) : that.createdAt != null) {
            return false;
        }
        if (dataType != null ? !dataType.equals(that.dataType) : that.dataType != null) {
            return false;
        }
        if (isLatest != null ? !isLatest.equals(that.isLatest) : that.isLatest != null) {
            return false;
        }
        if (library != null ? !library.equals(that.library) : that.library != null) {
            return false;
        }
        if (modifiedAt != null ? !modifiedAt.equals(that.modifiedAt) : that.modifiedAt != null) {
            return false;
        }
        if (project != null ? !project.equals(that.project) : that.project != null) {
            return false;
        }
        if (sample != null ? !sample.equals(that.sample) : that.sample != null) {
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
        result = 31 * result + (project != null ? project.hashCode() : 0);
        result = 31 * result + (sample != null ? sample.hashCode() : 0);
        result = 31 * result + (library != null ? library.hashCode() : 0);
        result = 31 * result + version;
        result = 31 * result + (createdAt != null ? createdAt.hashCode() : 0);
        result = 31 * result + (modifiedAt != null ? modifiedAt.hashCode() : 0);
        result = 31 * result + (isLatest != null ? isLatest.hashCode() : 0);
        result = 31 * result + (aggregationType != null ? aggregationType.hashCode() : 0);
        result = 31 * result + (workflowStartDate != null ? workflowStartDate.hashCode() : 0);
        result = 31 * result + (workflowEndDate != null ? workflowEndDate.hashCode() : 0);
        result = 31 * result + (dataType != null ? dataType.hashCode() : 0);
        return result;
    }

}
