package org.broadinstitute.gpinformatics.infrastructure.analytics.entity;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(schema = "MERCURYDW", name = "MAPPING_RUN_METRICS")
public class AlignmentMetric {
    @Id
    private Long mapping_run_metric_id;

    @Column(name = "run_name")
    private String runName;

    @Column(name = "run_date")
    private Date runDate;

    @Column(name = "analysis_node")
    private String analysisNode;

    @Column(name = "analysis_name")
    private String analysisName;

    @Column(name = "dragen_version")
    private String dragenVersion;

    @Column(name = "sample_alias")
    private String sampleAlias;

    @Column(name = "read_group")
    private String readGroup;

    @Column(name = "total_input_reads")
    private BigDecimal totalInputReads;

    @Column(name = "num_dup_marked_reads")
    private BigDecimal numDupMarkedReads;

    @Column(name = "num_dup_marked_removed")
    private String numDupMarkedReadsRemoved;

    @Column(name = "num_unique_reads")
    private BigDecimal numUniqueReads;

    @Column(name = "num_reads_mate_sequenced")
    private BigDecimal numReadsWithMateSequenced;

    @Column(name = "num_reads_wo_mate_sequenced")
    private BigDecimal numReadsWithoutMateSequenced;

    @Column(name = "num_qc_failed_reads")
    private BigDecimal numQcFailedReads;

    @Column(name = "num_mapped_reads")
    private BigDecimal numMappedReads;

    @Column(name = "num_mapped_reads_r1")
    private BigDecimal numMappedReadsR1;

    @Column(name = "num_mapped_reads_r2")
    private BigDecimal numMappedReadsR2;

    @Column(name = "num_unq_mapped_reads")
    private BigDecimal numUniqueMappedReads;

    @Column(name = "num_unmapped_reads")
    private BigDecimal numUmappedReads;

    @Column(name = "num_singleton_reads")
    private BigDecimal numSingletonReads;

    @Column(name = "num_paired_reads")
    private BigDecimal numPairedReads;

    @Column(name = "num_properly_paired_reads")
    private BigDecimal numProperlyPairedReads;

    @Column(name = "num_not_properly_paired_reads")
    private BigDecimal numNotProperlyPairedReads;

    @Column(name = "paired_reads_map_diff_chrom")
    private BigDecimal pairedReadsMapDifferentChromosome;

    @Column(name = "paired_reads_map_diff_chr_q10")
    private BigDecimal pairedReadsMapDifferentChromosomeQ10;

    @Column(name = "mapq_40_inf")
    private BigDecimal mapq40Inf;

    @Column(name = "mapq_30_40")
    private BigDecimal mapq3040;

    @Column(name = "mapq_20_30")
    private BigDecimal mapq2030;

    @Column(name = "mapq_10_20")
    private BigDecimal mapq1020;

    @Column(name = "mapq_0_10")
    private BigDecimal mapq020;

    @Column(name = "mapq_na")
    private BigDecimal mapqNa;

    @Column(name = "reads_indel_r1")
    private BigDecimal readsIndelR1;

    @Column(name = "reads_indel_r2")
    private BigDecimal readsIndelR2;

    @Column(name = "total_bases")
    private BigDecimal totalBases;

    @Column(name = "total_bases_r1")
    private BigDecimal totalBasesR1;

    @Column(name = "total_bases_r2")
    private BigDecimal totalBasesR2;

    @Column(name = "mapped_bases_r1")
    private BigDecimal mappedBasesR1;

    @Column(name = "mapped_bases_r2")
    private BigDecimal mappedBasesR2;

    @Column(name = "soft_clipped_bases_r1")
    private BigDecimal softClippedBasesR1;

    @Column(name = "soft_clipped_bases_r2")
    private BigDecimal softClippedBasesR2;

    @Column(name = "mismatched_bases_r1")
    private BigDecimal mismatchedBasesR1;

    @Column(name = "mismatched_bases_r2")
    private BigDecimal mismatchedBasesR2;

    @Column(name = "mismatched_bases_excl_r1")
    private BigDecimal mismatchedBasesExclR1;

    @Column(name = "mismatched_bases_excl_r2")
    private BigDecimal mismatchedBasesExclR2;

    @Column(name = "q30_bases")
    private BigDecimal q30Bases;

    @Column(name = "q30_bases_r1")
    private BigDecimal q30BasesR1;

    @Column(name = "q30_bases_r2")
    private BigDecimal q30BasesR2;

    @Column(name = "pct_coverage_20x_inf")
    private BigDecimal pctCov20x;

    @Column(name = "est_sample_contamination")
    private BigDecimal estimatedSampleContamination;

    @Column(name = "avg_alignment_coverage")
    private BigDecimal averageAlignmentCoverage;

    @Column(name = "predicted_sex_chr_ploidy")
    private String predictedSexChromosomePloidy;

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

    public String getAnalysisNode() {
        return analysisNode;
    }

    public void setAnalysisNode(String analysisNode) {
        this.analysisNode = analysisNode;
    }

    public String getAnalysisName() {
        return analysisName;
    }

    public void setAnalysisName(String analysisName) {
        this.analysisName = analysisName;
    }

    public String getDragenVersion() {
        return dragenVersion;
    }

    public void setDragenVersion(String dragenVersion) {
        this.dragenVersion = dragenVersion;
    }

    public String getSampleAlias() {
        return sampleAlias;
    }

    public void setSampleAlias(String sampleAlias) {
        this.sampleAlias = sampleAlias;
    }

    public String getReadGroup() {
        return readGroup;
    }

    public void setReadGroup(String readGroup) {
        this.readGroup = readGroup;
    }

    public BigDecimal getTotalInputReads() {
        return totalInputReads;
    }

    public void setTotalInputReads(BigDecimal totalInputReads) {
        this.totalInputReads = totalInputReads;
    }

    public BigDecimal getNumDupMarkedReads() {
        return numDupMarkedReads;
    }

    public void setNumDupMarkedReads(BigDecimal numDupMarkedReads) {
        this.numDupMarkedReads = numDupMarkedReads;
    }

    public String getNumDupMarkedReadsRemoved() {
        return numDupMarkedReadsRemoved;
    }

    public void setNumDupMarkedReadsRemoved(String numDupMarkedReadsRemoved) {
        this.numDupMarkedReadsRemoved = numDupMarkedReadsRemoved;
    }

    public BigDecimal getNumUniqueReads() {
        return numUniqueReads;
    }

    public void setNumUniqueReads(BigDecimal numUniqueReads) {
        this.numUniqueReads = numUniqueReads;
    }

    public BigDecimal getNumReadsWithMateSequenced() {
        return numReadsWithMateSequenced;
    }

    public void setNumReadsWithMateSequenced(BigDecimal numReadsWithMateSequenced) {
        this.numReadsWithMateSequenced = numReadsWithMateSequenced;
    }

    public BigDecimal getNumReadsWithoutMateSequenced() {
        return numReadsWithoutMateSequenced;
    }

    public void setNumReadsWithoutMateSequenced(BigDecimal numReadsWithoutMateSequenced) {
        this.numReadsWithoutMateSequenced = numReadsWithoutMateSequenced;
    }

    public BigDecimal getNumQcFailedReads() {
        return numQcFailedReads;
    }

    public void setNumQcFailedReads(BigDecimal numQcFailedReads) {
        this.numQcFailedReads = numQcFailedReads;
    }

    public BigDecimal getNumMappedReads() {
        return numMappedReads;
    }

    public void setNumMappedReads(BigDecimal numMappedReads) {
        this.numMappedReads = numMappedReads;
    }

    public BigDecimal getNumMappedReadsR1() {
        return numMappedReadsR1;
    }

    public void setNumMappedReadsR1(BigDecimal numMappedReadsR1) {
        this.numMappedReadsR1 = numMappedReadsR1;
    }

    public BigDecimal getNumMappedReadsR2() {
        return numMappedReadsR2;
    }

    public void setNumMappedReadsR2(BigDecimal numMappedReadsR2) {
        this.numMappedReadsR2 = numMappedReadsR2;
    }

    public BigDecimal getNumUniqueMappedReads() {
        return numUniqueMappedReads;
    }

    public void setNumUniqueMappedReads(BigDecimal numUniqueMappedReads) {
        this.numUniqueMappedReads = numUniqueMappedReads;
    }

    public BigDecimal getNumUmappedReads() {
        return numUmappedReads;
    }

    public void setNumUmappedReads(BigDecimal numUmappedReads) {
        this.numUmappedReads = numUmappedReads;
    }

    public BigDecimal getNumSingletonReads() {
        return numSingletonReads;
    }

    public void setNumSingletonReads(BigDecimal numSingletonReads) {
        this.numSingletonReads = numSingletonReads;
    }

    public BigDecimal getNumPairedReads() {
        return numPairedReads;
    }

    public void setNumPairedReads(BigDecimal numPairedReads) {
        this.numPairedReads = numPairedReads;
    }

    public BigDecimal getNumProperlyPairedReads() {
        return numProperlyPairedReads;
    }

    public void setNumProperlyPairedReads(BigDecimal numProperlyPairedReads) {
        this.numProperlyPairedReads = numProperlyPairedReads;
    }

    public BigDecimal getNumNotProperlyPairedReads() {
        return numNotProperlyPairedReads;
    }

    public void setNumNotProperlyPairedReads(BigDecimal numNotProperlyPairedReads) {
        this.numNotProperlyPairedReads = numNotProperlyPairedReads;
    }

    public BigDecimal getPairedReadsMapDifferentChromosome() {
        return pairedReadsMapDifferentChromosome;
    }

    public void setPairedReadsMapDifferentChromosome(BigDecimal pairedReadsMapDifferentChromosome) {
        this.pairedReadsMapDifferentChromosome = pairedReadsMapDifferentChromosome;
    }

    public BigDecimal getPairedReadsMapDifferentChromosomeQ10() {
        return pairedReadsMapDifferentChromosomeQ10;
    }

    public void setPairedReadsMapDifferentChromosomeQ10(BigDecimal pairedReadsMapDifferentChromosomeQ10) {
        this.pairedReadsMapDifferentChromosomeQ10 = pairedReadsMapDifferentChromosomeQ10;
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

    public BigDecimal getMapq020() {
        return mapq020;
    }

    public void setMapq020(BigDecimal mapq020) {
        this.mapq020 = mapq020;
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

    public BigDecimal getTotalBases() {
        return totalBases;
    }

    public void setTotalBases(BigDecimal totalBases) {
        this.totalBases = totalBases;
    }

    public BigDecimal getTotalBasesR1() {
        return totalBasesR1;
    }

    public void setTotalBasesR1(BigDecimal totalBasesR1) {
        this.totalBasesR1 = totalBasesR1;
    }

    public BigDecimal getTotalBasesR2() {
        return totalBasesR2;
    }

    public void setTotalBasesR2(BigDecimal totalBasesR2) {
        this.totalBasesR2 = totalBasesR2;
    }

    public BigDecimal getMappedBasesR1() {
        return mappedBasesR1;
    }

    public void setMappedBasesR1(BigDecimal mappedBasesR1) {
        this.mappedBasesR1 = mappedBasesR1;
    }

    public BigDecimal getMappedBasesR2() {
        return mappedBasesR2;
    }

    public void setMappedBasesR2(BigDecimal mappedBasesR2) {
        this.mappedBasesR2 = mappedBasesR2;
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

    public BigDecimal getMismatchedBasesR1() {
        return mismatchedBasesR1;
    }

    public void setMismatchedBasesR1(BigDecimal mismatchedBasesR1) {
        this.mismatchedBasesR1 = mismatchedBasesR1;
    }

    public BigDecimal getMismatchedBasesR2() {
        return mismatchedBasesR2;
    }

    public void setMismatchedBasesR2(BigDecimal mismatchedBasesR2) {
        this.mismatchedBasesR2 = mismatchedBasesR2;
    }

    public BigDecimal getMismatchedBasesExclR1() {
        return mismatchedBasesExclR1;
    }

    public void setMismatchedBasesExclR1(BigDecimal mismatchedBasesExclR1) {
        this.mismatchedBasesExclR1 = mismatchedBasesExclR1;
    }

    public BigDecimal getMismatchedBasesExclR2() {
        return mismatchedBasesExclR2;
    }

    public void setMismatchedBasesExclR2(BigDecimal mismatchedBasesExclR2) {
        this.mismatchedBasesExclR2 = mismatchedBasesExclR2;
    }

    public BigDecimal getQ30Bases() {
        return q30Bases;
    }

    public void setQ30Bases(BigDecimal q30Bases) {
        this.q30Bases = q30Bases;
    }

    public BigDecimal getQ30BasesR1() {
        return q30BasesR1;
    }

    public void setQ30BasesR1(BigDecimal q30BasesR1) {
        this.q30BasesR1 = q30BasesR1;
    }

    public BigDecimal getQ30BasesR2() {
        return q30BasesR2;
    }

    public void setQ30BasesR2(BigDecimal q30BasesR2) {
        this.q30BasesR2 = q30BasesR2;
    }

    public BigDecimal getPctCov20x() {
        return pctCov20x;
    }

    public void setPctCov20x(BigDecimal pctCov20x) {
        this.pctCov20x = pctCov20x;
    }

    public BigDecimal getEstimatedSampleContamination() {
        return estimatedSampleContamination;
    }

    public void setEstimatedSampleContamination(BigDecimal estimatedSampleContamination) {
        this.estimatedSampleContamination = estimatedSampleContamination;
    }

    public BigDecimal getAverageAlignmentCoverage() {
        return averageAlignmentCoverage;
    }

    public void setAverageAlignmentCoverage(BigDecimal averageAlignmentCoverage) {
        this.averageAlignmentCoverage = averageAlignmentCoverage;
    }

    public String getPredictedSexChromosomePloidy() {
        return predictedSexChromosomePloidy;
    }

    public void setPredictedSexChromosomePloidy(String predictedSexChromosomePloidy) {
        this.predictedSexChromosomePloidy = predictedSexChromosomePloidy;
    }

    public Long getMapping_run_metric_id() {
        return mapping_run_metric_id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || !(OrmUtil.proxySafeIsInstance(o, AlignmentMetric.class))) {
            return false;
        }

        AlignmentMetric that = OrmUtil.proxySafeCast(o, AlignmentMetric.class);

        return new EqualsBuilder().append(getSampleAlias(), that.getSampleAlias())
                .append(getRunName(), that.getRunName())
                .append(getRunDate(), that.getRunDate())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getSampleAlias())
                .append(getRunName())
                .append(getRunDate())
                .toHashCode();
    }
}
