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
import org.hibernate.annotations.BatchSize;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "AGGREGATION", schema = "METRICS")
public class Aggregation {
    @SuppressWarnings("unused")
    @Id @Column(name = "ID")
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

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "aggregation")
    @BatchSize(size = 100)
    private Set<AggregationAlignment> aggregationAlignments = new HashSet<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID", referencedColumnName = "AGGREGATION_ID", insertable = false, updatable = false)
    private AggregationContam aggregationContam;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID", referencedColumnName = "AGGREGATION_ID", insertable = false, updatable = false)
    private AggregationHybridSelection aggregationHybridSelection;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "aggregation")
    @BatchSize(size = 100)
    private Set<AggregationReadGroup> aggregationReadGroups = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID", referencedColumnName = "AGGREGATION_ID", insertable = false, updatable = false)
    private AggregationWgs aggregationWgs;

    @Transient
    private LevelOfDetection levelOfDetection;

    public Aggregation() {
    }

    public Aggregation(String project, String sample, String library, Integer version, Integer readGroupCount,
                       String dataType,
                       Set<AggregationAlignment> aggregationAlignments,
                       AggregationContam aggregationContam,
                       AggregationHybridSelection aggregationHybridSelection,
                       Set<AggregationReadGroup> aggregationReadGroups,
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

    public Aggregation(String project, String sample, Integer version) {
        this.project = project;
        this.sample = sample;
        this.version = version;
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

    public Integer getId() {
        return id;
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

    public Set<AggregationAlignment> getAggregationAlignments() {
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

    public Set<AggregationReadGroup> getAggregationReadGroups() {
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
}
