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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTuple;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionLibraryDescriptor;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
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
    public static final String DATA_TYPE_EXOME = "Exome";
    public static final String DATA_TYPE_RNA = "RNA";
    public static final String DATA_TYPE_WGS = "WGS";
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

    @Column(name = "PROCESSING_LOCATION")
    private String processingLocation;

    @Column(name = "IS_LATEST")
    private boolean latest;

    @Column(name = "DATA_TYPE")
    private String dataType;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "aggregation")
    @BatchSize(size = 100)
    private Set<AggregationAlignment> aggregationAlignments = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID", referencedColumnName = "AGGREGATION_ID", insertable = false, updatable = false)
    private AggregationContam aggregationContam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID", referencedColumnName = "AGGREGATION_ID", insertable = false, updatable = false)
    private AggregationHybridSelection aggregationHybridSelection;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "aggregation")
    @BatchSize(size = 100)
    private Set<AggregationReadGroup> aggregationReadGroups = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID", referencedColumnName = "AGGREGATION_ID", insertable = false, updatable = false)
    private AggregationWgs aggregationWgs;

    @Transient
    private LevelOfDetection levelOfDetection;
    @Transient
    private SubmissionTuple submissionTuple;

    public Aggregation() {
    }

    public Aggregation(String project, String sample, String library, Integer version, Integer readGroupCount,
                       String dataType,
                       Set<AggregationAlignment> aggregationAlignments,
                       AggregationContam aggregationContam,
                       AggregationHybridSelection aggregationHybridSelection,
                       Set<AggregationReadGroup> aggregationReadGroups,
                       AggregationWgs aggregationWgs,
                       LevelOfDetection levelOfDetection,
                       String processingLocation) {
        this.project = project;
        this.sample = sample;
        this.library = library;
        this.version = version;
        this.readGroupCount = readGroupCount;
        this.dataType = dataType;
        this.processingLocation = processingLocation;
        this.aggregationAlignments = aggregationAlignments;
        this.aggregationContam = aggregationContam;
        this.aggregationHybridSelection = aggregationHybridSelection;
        this.aggregationReadGroups = aggregationReadGroups;
        this.aggregationWgs = aggregationWgs;
        this.levelOfDetection = levelOfDetection;
    }


    @Transient
    public SubmissionTuple getTuple() {
        // These aggregation metrics are specific to BAM files, so the BassFileType is always BAM.
        if (submissionTuple == null) {
            submissionTuple =
                new SubmissionTuple(getProject(), getSample(), getVersion().toString(), getProcessingLocation(), getDataType());
        }
        return submissionTuple;
    }

    public Double getQualityMetric() {
        switch (dataType) {
        case DATA_TYPE_EXOME:
            return aggregationHybridSelection.getPctTargetBases20X();
        case DATA_TYPE_RNA:
            long totalReadsAlignedInPairs = 0;
            for (AggregationAlignment aggregationAlignment : getAggregationAlignments()) {
                if (aggregationAlignment.getCategory().equals("PAIR")) {
                    totalReadsAlignedInPairs = aggregationAlignment.getPfAlignedBases();
                }
            }
            return (double) totalReadsAlignedInPairs;
        case DATA_TYPE_WGS:
            if (aggregationWgs.getMeanCoverage()!=0){
                return aggregationWgs.getMeanCoverage();
            }
        default:
            return null;
        }
    }

    public String getQualityMetricString() {
        if (dataType == null) {
            return null;
        }
        Double qualityMetric = getQualityMetric();
        switch (dataType) {
        case DATA_TYPE_EXOME:
            return convertToPercent(qualityMetric);
        case DATA_TYPE_RNA:
            return MessageFormat.format("{0,number,#}", qualityMetric);
        case DATA_TYPE_WGS:
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

    public String getProcessingLocation() {
        return processingLocation;
    }

    public void setProcessingLocation(String processingLocation) {
        this.processingLocation = processingLocation;
    }

    public boolean isLatest() {
        return latest;
    }

    public void setLatest(boolean latest) {
        this.latest = latest;
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

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || (!OrmUtil.proxySafeIsInstance(o, Aggregation.class))) {
            return false;
        }

        if (!(o instanceof Aggregation)) {
            return false;
        }

        Aggregation that = OrmUtil.proxySafeCast(o, Aggregation.class);

        return new EqualsBuilder()
            .append(latest, that.latest)
            .append(id, that.id)
            .append(project, that.project)
            .append(sample, that.sample)
            .append(library, that.library)
            .append(version, that.version)
            .append(readGroupCount, that.readGroupCount)
            .append(processingLocation, that.processingLocation)
            .append(dataType, that.dataType)
            .append(aggregationAlignments, that.aggregationAlignments)
            .append(aggregationContam, that.aggregationContam)
            .append(aggregationHybridSelection, that.aggregationHybridSelection)
            .append(aggregationReadGroups, that.aggregationReadGroups)
            .append(aggregationWgs, that.aggregationWgs)
            .append(levelOfDetection, that.levelOfDetection)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(id)
            .append(project)
            .append(sample)
            .append(library)
            .append(version)
            .append(readGroupCount)
            .append(processingLocation)
            .append(latest)
            .append(dataType)
            .append(aggregationAlignments)
            .append(aggregationContam)
            .append(aggregationHybridSelection)
            .append(aggregationReadGroups)
            .append(aggregationWgs)
            .append(levelOfDetection)
            .toHashCode();
    }

    public Set<SubmissionLibraryDescriptor> getLibraryTypes() {
        Set<SubmissionLibraryDescriptor> submissionLibraryDescriptors = new HashSet<>();
        for (AggregationReadGroup aggregationReadGroup : getAggregationReadGroups()) {
            ReadGroupIndex readGroupIndex = aggregationReadGroup.getReadGroupIndex();
            if (readGroupIndex != null) {
                submissionLibraryDescriptors.add(readGroupIndex.getLibraryType());
            }
        }
        return submissionLibraryDescriptors;
    }
}
