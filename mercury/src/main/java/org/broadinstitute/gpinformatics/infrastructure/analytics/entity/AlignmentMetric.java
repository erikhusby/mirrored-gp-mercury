package org.broadinstitute.gpinformatics.infrastructure.analytics.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(schema = "MERCURYDW", name = "MAPPING_RG_METRICS")
public class AlignmentMetric {

    @Id
    private Long mappingRgMetricId;

    @Column(name = "run_name")
    private String runName;

    @Column(name = "run_date")
    private Date runDate;

    @Column(name = "sample_alias")
    private String sampleAlias;

    @Column(name = "total_reads")
    private BigDecimal totalReads;

    @Column(name = "num_dup_marked_reads")
    private BigDecimal numberOfDuplicateMarkedReads;

    @Column(name = "num_dup_marked_removed")
    private String numberOfDuplicateMarkedRemoved;

    @Column(name = "num_unique_reads")
    private BigDecimal numberOfUniqueReads;

    @Column(name = "num_reads_mate_sequenced")
    private BigDecimal numberOfReadsMateSequenced;

    @Column(name = "num_reads_wo_mate_sequenced")
    private BigDecimal numberOfReadsWithoutMateSequenced;

    @Column(name = "num_qc_failed_reads")
    private BigDecimal numberOfQcFailedReads;

    @Column(name = "num_mapped_reads")
    private BigDecimal numberOfMappedReads;

    @Column(name = "num_unq_mapped_reads")
    private BigDecimal numberOfUniqueMappedReads;

    @Column(name = "num_unmapped_reads")
    private BigDecimal numberOfUnmappedReads;

    @Column(name = "num_singleton_reads")
    private BigDecimal numberOfSingletonReads;

    @Column(name = "num_paired_reads")
    private BigDecimal numberOfPairedReads;

    @Column(name = "num_properly_paired_reads")
    private BigDecimal numberOfProperlyPairedReads;

    @Column(name = "num_not_properly_paired_reads")
    private BigDecimal numberOfNotProperlyPairedReads;

    @Column(name = "mapq_40_inf")
    private BigDecimal mapq40Inf;

    @Column(name = "mapq_30_40")
    private BigDecimal mapq3040;

    @Column(name = "mapq_20_30")
    private BigDecimal mapq2030;

    @Column(name = "mapq_10_20")
    private BigDecimal mapq1020;

    @Column(name = "mapq_0_10")
    private BigDecimal mapq010;

    @Column(name = "mapq_na")
    private BigDecimal mapqNa;

    @Column(name = "reads_indel_r1")
    private BigDecimal readsIndelR1;

    @Column(name = "reads_indel_r2")
    private BigDecimal readsIndelR2;

    @Column(name = "soft_clipped_bases_r1")
    private BigDecimal softClippedBasesR1;

    @Column(name = "soft_clipped_bases_r2")
    private BigDecimal softClippedBasesR2;

    @Column(name = "total_alignments")
    private BigDecimal totalAlignments;

    @Column(name = "secondary_alignments")
    private BigDecimal secondaryAlignments;

    @Column(name = "supplementary_alignments")
    private BigDecimal supplementaryAlignments;

    @Column(name = "est_read_length")
    private BigDecimal estimatedReadLength;

    @Column(name = "avg_seq_coverage")
    private BigDecimal averageSequencingCoverage;

    @Column(name = "insert_length_mean")
    private BigDecimal insertLengthMean;

    @Column(name = "insert_length_std")
    private BigDecimal insertLengthStd;

    public AlignmentMetric() {
    }

    public String getRunName() {
        return runName;
    }

    public void setRunName(String runName) {
        this.runName = runName;
    }

    public Date getRunDate() {
        return runDate;
    }

    public void setRunDate(Date runDate) {
        this.runDate = runDate;
    }

    public String getSampleAlias() {
        return sampleAlias;
    }

    public void setSampleAlias(String sampleAlias) {
        this.sampleAlias = sampleAlias;
    }

    public BigDecimal getTotalReads() {
        return totalReads;
    }

    public void setTotalReads(BigDecimal totalReads) {
        this.totalReads = totalReads;
    }

    public BigDecimal getNumberOfDuplicateMarkedReads() {
        return numberOfDuplicateMarkedReads;
    }

    public void setNumberOfDuplicateMarkedReads(BigDecimal numberOfDuplicateMarkedReads) {
        this.numberOfDuplicateMarkedReads = numberOfDuplicateMarkedReads;
    }

    public String getNumberOfDuplicateMarkedRemoved() {
        return numberOfDuplicateMarkedRemoved;
    }

    public void setNumberOfDuplicateMarkedRemoved(String numberOfDuplicateMarkedRemoved) {
        this.numberOfDuplicateMarkedRemoved = numberOfDuplicateMarkedRemoved;
    }

    public BigDecimal getNumberOfUniqueReads() {
        return numberOfUniqueReads;
    }

    public void setNumberOfUniqueReads(BigDecimal numberOfUniqueReads) {
        this.numberOfUniqueReads = numberOfUniqueReads;
    }

    public BigDecimal getNumberOfReadsMateSequenced() {
        return numberOfReadsMateSequenced;
    }

    public void setNumberOfReadsMateSequenced(BigDecimal numberOfReadsMateSequenced) {
        this.numberOfReadsMateSequenced = numberOfReadsMateSequenced;
    }

    public BigDecimal getNumberOfReadsWithoutMateSequenced() {
        return numberOfReadsWithoutMateSequenced;
    }

    public void setNumberOfReadsWithoutMateSequenced(BigDecimal numberOfReadsWithoutMateSequenced) {
        this.numberOfReadsWithoutMateSequenced = numberOfReadsWithoutMateSequenced;
    }

    public BigDecimal getNumberOfQcFailedReads() {
        return numberOfQcFailedReads;
    }

    public void setNumberOfQcFailedReads(BigDecimal numberOfQcFailedReads) {
        this.numberOfQcFailedReads = numberOfQcFailedReads;
    }

    public BigDecimal getNumberOfMappedReads() {
        return numberOfMappedReads;
    }

    public void setNumberOfMappedReads(BigDecimal numberOfMappedReads) {
        this.numberOfMappedReads = numberOfMappedReads;
    }

    public BigDecimal getNumberOfUniqueMappedReads() {
        return numberOfUniqueMappedReads;
    }

    public void setNumberOfUniqueMappedReads(BigDecimal numberOfUniqueMappedReads) {
        this.numberOfUniqueMappedReads = numberOfUniqueMappedReads;
    }

    public BigDecimal getNumberOfUnmappedReads() {
        return numberOfUnmappedReads;
    }

    public void setNumberOfUnmappedReads(BigDecimal numberOfUnmappedReads) {
        this.numberOfUnmappedReads = numberOfUnmappedReads;
    }

    public BigDecimal getNumberOfSingletonReads() {
        return numberOfSingletonReads;
    }

    public void setNumberOfSingletonReads(BigDecimal numberOfSingletonReads) {
        this.numberOfSingletonReads = numberOfSingletonReads;
    }

    public BigDecimal getNumberOfPairedReads() {
        return numberOfPairedReads;
    }

    public void setNumberOfPairedReads(BigDecimal numberOfPairedReads) {
        this.numberOfPairedReads = numberOfPairedReads;
    }

    public BigDecimal getNumberOfProperlyPairedReads() {
        return numberOfProperlyPairedReads;
    }

    public void setNumberOfProperlyPairedReads(BigDecimal numberOfProperlyPairedReads) {
        this.numberOfProperlyPairedReads = numberOfProperlyPairedReads;
    }

    public BigDecimal getNumberOfNotProperlyPairedReads() {
        return numberOfNotProperlyPairedReads;
    }

    public void setNumberOfNotProperlyPairedReads(BigDecimal numberOfNotProperlyPairedReads) {
        this.numberOfNotProperlyPairedReads = numberOfNotProperlyPairedReads;
    }

    public BigDecimal getMapq40Inf() {
        return mapq40Inf;
    }

    public void setMapq40Inf(BigDecimal mapq40Inf) {
        this.mapq40Inf = mapq40Inf;
    }

    public BigDecimal getMapq3040() {
        return mapq3040;
    }

    public void setMapq3040(BigDecimal mapq3040) {
        this.mapq3040 = mapq3040;
    }

    public BigDecimal getMapq2030() {
        return mapq2030;
    }

    public void setMapq2030(BigDecimal mapq2030) {
        this.mapq2030 = mapq2030;
    }

    public BigDecimal getMapq1020() {
        return mapq1020;
    }

    public void setMapq1020(BigDecimal mapq1020) {
        this.mapq1020 = mapq1020;
    }

    public BigDecimal getMapq010() {
        return mapq010;
    }

    public void setMapq010(BigDecimal mapq010) {
        this.mapq010 = mapq010;
    }

    public BigDecimal getMapqNa() {
        return mapqNa;
    }

    public void setMapqNa(BigDecimal mapqNa) {
        this.mapqNa = mapqNa;
    }

    public BigDecimal getReadsIndelR1() {
        return readsIndelR1;
    }

    public void setReadsIndelR1(BigDecimal readsIndelR1) {
        this.readsIndelR1 = readsIndelR1;
    }

    public BigDecimal getReadsIndelR2() {
        return readsIndelR2;
    }

    public void setReadsIndelR2(BigDecimal readsIndelR2) {
        this.readsIndelR2 = readsIndelR2;
    }

    public BigDecimal getSoftClippedBasesR1() {
        return softClippedBasesR1;
    }

    public void setSoftClippedBasesR1(BigDecimal softClippedBasesR1) {
        this.softClippedBasesR1 = softClippedBasesR1;
    }

    public BigDecimal getSoftClippedBasesR2() {
        return softClippedBasesR2;
    }

    public void setSoftClippedBasesR2(BigDecimal softClippedBasesR2) {
        this.softClippedBasesR2 = softClippedBasesR2;
    }

    public BigDecimal getTotalAlignments() {
        return totalAlignments;
    }

    public void setTotalAlignments(BigDecimal totalAlignments) {
        this.totalAlignments = totalAlignments;
    }

    public BigDecimal getSecondaryAlignments() {
        return secondaryAlignments;
    }

    public void setSecondaryAlignments(BigDecimal secondaryAlignments) {
        this.secondaryAlignments = secondaryAlignments;
    }

    public BigDecimal getSupplementaryAlignments() {
        return supplementaryAlignments;
    }

    public void setSupplementaryAlignments(BigDecimal supplementaryAlignments) {
        this.supplementaryAlignments = supplementaryAlignments;
    }

    public BigDecimal getEstimatedReadLength() {
        return estimatedReadLength;
    }

    public void setEstimatedReadLength(BigDecimal estimatedReadLength) {
        this.estimatedReadLength = estimatedReadLength;
    }

    public BigDecimal getAverageSequencingCoverage() {
        return averageSequencingCoverage;
    }

    public void setAverageSequencingCoverage(BigDecimal averageSequencingCoverage) {
        this.averageSequencingCoverage = averageSequencingCoverage;
    }

    public BigDecimal getInsertLengthMean() {
        return insertLengthMean;
    }

    public void setInsertLengthMean(BigDecimal insertLengthMean) {
        this.insertLengthMean = insertLengthMean;
    }

    public BigDecimal getInsertLengthStd() {
        return insertLengthStd;
    }

    public void setInsertLengthStd(BigDecimal insertLengthStd) {
        this.insertLengthStd = insertLengthStd;
    }
}
