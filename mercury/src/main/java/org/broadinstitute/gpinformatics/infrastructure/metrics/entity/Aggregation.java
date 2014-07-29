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

import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;

import javax.persistence.Column;
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

@Entity
@Table(name = "AGGREGATION", schema = "METRICS")
public class Aggregation {
    @SuppressWarnings("unused")
    @Id
    private Integer id;

    @Column(name="PROJECT")
    private String project;

    @Column(name="SAMPLE")
    private String sample;

    @SuppressWarnings("unused")
    @Column(name = "LIBRARY")
    private String library;

    @Column(name="VERSION")
    private Integer version;

    @Column(name="READ_GROUP_COUNT")
    private Integer readGroupCount;

    @Transient
    private String dataType;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "aggregation")
    private Collection<AggregationAlignment> aggregationAlignments = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "aggregation", optional = false)
    private AggregationContam aggregationContam;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "aggregation", optional = false)
    private AggregationHybridSelection aggregationHybridSelection;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "aggregation")
    private Collection<AggregationReadGroup> aggregationReadGroups = new ArrayList<>();

    @OneToOne(mappedBy = "aggregation")
    private AggregationWgs aggregationWgs;

    @Transient
    private LevelOfDetection levelOfDetection;

    public Aggregation() {
    }

    public Aggregation(String project, String sample, String library, Integer version, Integer readGroupCount,
                       String dataType,
                       Collection<AggregationAlignment> aggregationAlignments,
                       AggregationContam aggregationContam,
                       AggregationHybridSelection aggregationHybridSelection,
                       Collection<AggregationReadGroup> aggregationReadGroups,
                       AggregationWgs aggregationWgs,
                       LevelOfDetection levelOfDetection) {
        this.project = project;
        this.sample = sample;
        this.library = library;
        this.version = version;
        this.readGroupCount = readGroupCount;
        this.dataType = dataType;
        this.aggregationAlignments = aggregationAlignments;
        this.aggregationContam = aggregationContam;
        this.aggregationHybridSelection = aggregationHybridSelection;
        this.aggregationReadGroups = aggregationReadGroups;
        this.aggregationWgs = aggregationWgs;
        this.levelOfDetection = levelOfDetection;
    }

    public Double getQualityMetric(String dataType) {
        switch (dataType) {
        case BassDTO.DATA_TYPE_EXOME:
            return aggregationHybridSelection.getPctTargetBases20X();
        case BassDTO.DATA_TYPE_RNA:
            long totalReadsAlignedInPairs = 0;
            for (AggregationAlignment aggregationAlignment : getAggregationAlignments()) {
                if (aggregationAlignment.getCategory().equals("PAIR")) {
                    totalReadsAlignedInPairs = aggregationAlignment.getPfAlignedBases();
                }
            }
            return (double) totalReadsAlignedInPairs;
        case BassDTO.DATA_TYPE_WGS:
            if (aggregationWgs.getMeanCoverage()!=0){
                return aggregationWgs.getMeanCoverage();
            }
        default:
            return null;
        }
    }

    public String getQualityMetricString(String dataType) {
        if (dataType == null) {
            return null;
        }
        Double qualityMetric = getQualityMetric(dataType);
        switch (dataType) {
        case BassDTO.DATA_TYPE_EXOME:
            return convertToPercent(qualityMetric);
        case BassDTO.DATA_TYPE_RNA:
            return MessageFormat.format("{0,number,#}", qualityMetric);
        case BassDTO.DATA_TYPE_WGS:
            return MessageFormat.format("{0,number,#.##}", qualityMetric);
        default:
            return "N/A";
        }
    }

    public String getLibrary() {
        return library;
    }

    protected String convertToPercent(double decimalValue) {
        return MessageFormat.format("{0,number,#.##%}", decimalValue);
    }

    public String getProject() {
        return project;
    }

    public String getSample() {
        return sample;
    }
    public Integer getVersion() {
        return version;
    }

    public Integer getReadGroupCount() {
        return readGroupCount;
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

    public AggregationContam getAggregationContam() {
        return aggregationContam;
    }

    public String getContaminationString() {
        if (aggregationContam != null && aggregationContam.getPctContamination() != null) {
            return convertToPercent(aggregationContam.getPctContamination());
        } else {
            return "N/A";
        }
    }

    public AggregationHybridSelection getAggregationHybridSelection() {
        return aggregationHybridSelection;
    }

    public Collection<AggregationReadGroup> getAggregationReadGroups() {
        return aggregationReadGroups;
    }

    public AggregationWgs getAggregationWgs() {
        return aggregationWgs;
    }

    public LevelOfDetection getLevelOfDetection() {
        return levelOfDetection;
    }

    public void setLevelOfDetection(LevelOfDetection levelOfDetection) {
        this.levelOfDetection = levelOfDetection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Aggregation)) {
            return false;
        }

        Aggregation that = (Aggregation) o;

        if (!id.equals(that.id)) {
            return false;
        }
        if (!version.equals(that.version)) {
            return false;
        }
        if (aggregationAlignments != null ? !aggregationAlignments.equals(that.aggregationAlignments) :
                that.aggregationAlignments != null) {
            return false;
        }
        if (aggregationContam != null ? !aggregationContam.equals(that.aggregationContam) :
                that.aggregationContam != null) {
            return false;
        }
        if (aggregationHybridSelection != null ? !aggregationHybridSelection.equals(that.aggregationHybridSelection) :
                that.aggregationHybridSelection != null) {
            return false;
        }
        if (aggregationReadGroups != null ? !aggregationReadGroups.equals(that.aggregationReadGroups) :
                that.aggregationReadGroups != null) {
            return false;
        }
        if (aggregationWgs != null ? !aggregationWgs.equals(that.aggregationWgs) : that.aggregationWgs != null) {
            return false;
        }
        if (dataType != null ? !dataType.equals(that.dataType) : that.dataType != null) {
            return false;
        }
        if (levelOfDetection != null ? !levelOfDetection.equals(that.levelOfDetection) :
                that.levelOfDetection != null) {
            return false;
        }
        if (library != null ? !library.equals(that.library) : that.library != null) {
            return false;
        }
        if (project != null ? !project.equals(that.project) : that.project != null) {
            return false;
        }
        if (readGroupCount != null ? !readGroupCount.equals(that.readGroupCount) : that.readGroupCount != null) {
            return false;
        }
        return !(sample != null ? !sample.equals(that.sample) : that.sample != null);

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (project != null ? project.hashCode() : 0);
        result = 31 * result + (sample != null ? sample.hashCode() : 0);
        result = 31 * result + (library != null ? library.hashCode() : 0);
        result = 31 * result + version;
        result = 31 * result + (readGroupCount != null ? readGroupCount.hashCode() : 0);
        result = 31 * result + (dataType != null ? dataType.hashCode() : 0);
        result = 31 * result + (aggregationAlignments != null ? aggregationAlignments.hashCode() : 0);
        result = 31 * result + (aggregationContam != null ? aggregationContam.hashCode() : 0);
        result = 31 * result + (aggregationHybridSelection != null ? aggregationHybridSelection.hashCode() : 0);
        result = 31 * result + (aggregationReadGroups != null ? aggregationReadGroups.hashCode() : 0);
        result = 31 * result + (aggregationWgs != null ? aggregationWgs.hashCode() : 0);
        result = 31 * result + (levelOfDetection != null ? levelOfDetection.hashCode() : 0);
        return result;
    }
}
