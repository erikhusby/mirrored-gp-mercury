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
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigInteger;

@Entity
@Table(name = "AGGREGATION_HYBRID_SELECTION", schema = "METRICS")
public class AggregationHybridSelection implements Serializable {
    @Id
    @Column(name = "AGGREGATION_ID", nullable = false, insertable = false, updatable = false)
    private int aggregationId;
    @Column(name = "bait_set")
    private String baitSet;
    @Column(name = "GENOME_SIZE")
    private BigInteger genomeSize;
    @Column(name = "BAIT_TERRITORY")
    private BigInteger baitTerritory;
    @Column(name = "target_territory")
    private BigInteger targetTerritory;
    @Column(name = "BAIT_DESIGN_EFFICIENCY")
    private Double baitDesignEfficiency;
    @Column(name = "total_reads")
    private BigInteger totalReads;
    @Column(name = "PF_READS")
    private BigInteger pfReads;
    @Column(name = "PF_UNIQUE_READS")
    private BigInteger pfUniqueReads;
    @Column(name = "pct_pf_reads")
    private Double pctPfReads;
    @Column(name = "PCT_PF_UQ_READS")
    private Double pctPfUqReads;
    @Column(name = "PF_UQ_READS_ALIGNED")
    private BigInteger pfUqReadsAligned;
    @Column(name = "PCT_PF_UQ_READS_ALIGNED")
    private Double pctPfUqReadsAligned;
    @Column(name = "PF_UQ_BASES_ALIGNED")
    private BigInteger pfUqBasesAligned;
    @Column(name = "ON_BAIT_BASES")
    private BigInteger onBaitBases;
    @Column(name = "NEAR_BAIT_BASES")
    private BigInteger nearBaitBases;
    @Column(name = "OFF_BAIT_BASES")
    private BigInteger offBaitBases;
    @Column(name = "ON_TARGET_BASES")
    private BigInteger onTargetBases;
    @Column(name = "PCT_SELECTED_BASES")
    private Double pctSelectedBases;
    @Column(name = "PCT_OFF_BAIT")
    private Double pctOffBait;
    @Column(name = "ON_BAIT_VS_SELECTED")
    private Double onBaitVsSelected;
    @Column(name = "MEAN_BAIT_COVERAGE")
    private Double meanBaitCoverage;
    @Column(name = "MEAN_TARGET_COVERAGE")
    private Double meanTargetCoverage;
    @Column(name = "PCT_USABLE_BASES_ON_BAIT")
    private Double pctUsableBasesOnBait;
    @Column(name = "PCT_USABLE_BASES_ON_TARGET")
    private Double pctUsableBasesOnTarget;
    @Column(name = "FOLD_ENRICHMENT")
    private Double foldEnrichment;
    @Column(name = "ZERO_CVG_TARGETS_PCT")
    private Double zeroCvgTargetsPct;
    @Column(name = "FOLD_80_BASE_PENALTY")
    private Double fold80BasePenalty;
    @Column(name = "PCT_TARGET_BASES_2X")
    private Double pctTargetBases2X;
    @Column(name = "PCT_TARGET_BASES_10X")
    private Double pctTargetBases10X;
    @Column(name = "PCT_TARGET_BASES_20X")
    private Double pctTargetBases20X;
    @Column(name = "PCT_TARGET_BASES_30X")
    private Double pctTargetBases30X;
    @Column(name = "HS_LIBRARY_SIZE")
    private BigInteger hsLibrarySize;
    @Column(name = "HS_PENALTY_10X")
    private Double hsPenalty10X;
    @Column(name = "HS_PENALTY_20X")
    private Double hsPenalty20X;
    @Column(name = "HS_PENALTY_30X")
    private Double hsPenalty30X;
    @Column(name = "AT_DROPOUT")
    private Double atDropout;
    @Column(name = "GC_DROPOUT")
    private Double gcDropout;
    @Column(name = "HS_PENALTY_40X")
    private Double hsPenalty40X;
    @Column(name = "HS_PENALTY_50X")
    private Double hsPenalty50X;
    @Column(name = "HS_PENALTY_100X")
    private Double hsPenalty100X;
    @Column(name = "PCT_TARGET_BASES_40X")
    private Double percentTargetBases40X;
    @Column(name = "PCT_TARGET_BASES_50X")
    private Double percentTargetBases50X;
    @Column(name = "PCT_TARGET_BASES_100X")
    private Double pctTargetBases100X;
    @OneToOne
    @JoinColumn(name = "AGGREGATION_ID", referencedColumnName = "ID", nullable = false)
    private Aggregation aggregation;

    public Double getPctPfUqReads() {
        return pctPfUqReads;
    }

    public String getBaitSet() {
        return baitSet;
    }

    public BigInteger getGenomeSize() {
        return genomeSize;
    }

    public BigInteger getBaitTerritory() {
        return baitTerritory;
    }

    public BigInteger getTargetTerritory() {
        return targetTerritory;
    }

    public Double getBaitDesignEfficiency() {
        return baitDesignEfficiency;
    }

    public BigInteger getTotalReads() {
        return totalReads;
    }

    public BigInteger getPfReads() {
        return pfReads;
    }

    public BigInteger getPfUniqueReads() {
        return pfUniqueReads;
    }

    public Double getPctPfReads() {
        return pctPfReads;
    }

    public BigInteger getPfUqReadsAligned() {
        return pfUqReadsAligned;
    }

    public Double getPctPfUqReadsAligned() {
        return pctPfUqReadsAligned;
    }

    public BigInteger getPfUqBasesAligned() {
        return pfUqBasesAligned;
    }

    public BigInteger getOnBaitBases() {
        return onBaitBases;
    }

    public BigInteger getNearBaitBases() {
        return nearBaitBases;
    }

    public BigInteger getOffBaitBases() {
        return offBaitBases;
    }

    public BigInteger getOnTargetBases() {
        return onTargetBases;
    }

    public Double getPctSelectedBases() {
        return pctSelectedBases;
    }

    public Double getPctOffBait() {
        return pctOffBait;
    }

    public Double getOnBaitVsSelected() {
        return onBaitVsSelected;
    }

    public Double getMeanBaitCoverage() {
        return meanBaitCoverage;
    }

    public Double getMeanTargetCoverage() {
        return meanTargetCoverage;
    }

    public Double getPctUsableBasesOnBait() {
        return pctUsableBasesOnBait;
    }

    public Double getPctUsableBasesOnTarget() {
        return pctUsableBasesOnTarget;
    }

    public Double getFoldEnrichment() {
        return foldEnrichment;
    }

    public Double getZeroCvgTargetsPct() {
        return zeroCvgTargetsPct;
    }

    public Double getFold80BasePenalty() {
        return fold80BasePenalty;
    }

    public Double getPctTargetBases2X() {
        return pctTargetBases2X;
    }

    public Double getPctTargetBases10X() {
        return pctTargetBases10X;
    }

    public Double getPctTargetBases20X() {
        return pctTargetBases20X;
    }

    public Double getPctTargetBases30X() {
        return pctTargetBases30X;
    }

    public BigInteger getHsLibrarySize() {
        return hsLibrarySize;
    }

    public Double getHsPenalty10X() {
        return hsPenalty10X;
    }

    public Double getHsPenalty20X() {
        return hsPenalty20X;
    }

    public Double getHsPenalty30X() {
        return hsPenalty30X;
    }

    public Double getAtDropout() {
        return atDropout;
    }

    public Double getGcDropout() {
        return gcDropout;
    }

    public Double getHsPenalty40X() {
        return hsPenalty40X;
    }

    public Double getHsPenalty50X() {
        return hsPenalty50X;
    }

    public Double getHsPenalty100X() {
        return hsPenalty100X;
    }

    public Double getPercentTargetBases40X() {
        return percentTargetBases40X;
    }

    public Double getPercentTargetBases50X() {
        return percentTargetBases50X;
    }

    public Double getPctTargetBases100X() {
        return pctTargetBases100X;
    }

    public Aggregation getAggregation() {
        return aggregation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AggregationHybridSelection that = (AggregationHybridSelection) o;

        if (aggregationId != that.aggregationId) {
            return false;
        }
        if (atDropout != null ? !atDropout.equals(that.atDropout) : that.atDropout != null) {
            return false;
        }
        if (baitDesignEfficiency != null ? !baitDesignEfficiency.equals(that.baitDesignEfficiency) :
                that.baitDesignEfficiency != null) {
            return false;
        }
        if (baitSet != null ? !baitSet.equals(that.baitSet) : that.baitSet != null) {
            return false;
        }
        if (baitTerritory != null ? !baitTerritory.equals(that.baitTerritory) : that.baitTerritory != null) {
            return false;
        }
        if (fold80BasePenalty != null ? !fold80BasePenalty.equals(that.fold80BasePenalty) :
                that.fold80BasePenalty != null) {
            return false;
        }
        if (foldEnrichment != null ? !foldEnrichment.equals(that.foldEnrichment) : that.foldEnrichment != null) {
            return false;
        }
        if (gcDropout != null ? !gcDropout.equals(that.gcDropout) : that.gcDropout != null) {
            return false;
        }
        if (genomeSize != null ? !genomeSize.equals(that.genomeSize) : that.genomeSize != null) {
            return false;
        }
        if (hsLibrarySize != null ? !hsLibrarySize.equals(that.hsLibrarySize) : that.hsLibrarySize != null) {
            return false;
        }
        if (hsPenalty100X != null ? !hsPenalty100X.equals(that.hsPenalty100X) : that.hsPenalty100X != null) {
            return false;
        }
        if (hsPenalty10X != null ? !hsPenalty10X.equals(that.hsPenalty10X) : that.hsPenalty10X != null) {
            return false;
        }
        if (hsPenalty20X != null ? !hsPenalty20X.equals(that.hsPenalty20X) : that.hsPenalty20X != null) {
            return false;
        }
        if (hsPenalty30X != null ? !hsPenalty30X.equals(that.hsPenalty30X) : that.hsPenalty30X != null) {
            return false;
        }
        if (hsPenalty40X != null ? !hsPenalty40X.equals(that.hsPenalty40X) : that.hsPenalty40X != null) {
            return false;
        }
        if (hsPenalty50X != null ? !hsPenalty50X.equals(that.hsPenalty50X) : that.hsPenalty50X != null) {
            return false;
        }
        if (meanBaitCoverage != null ? !meanBaitCoverage.equals(that.meanBaitCoverage) :
                that.meanBaitCoverage != null) {
            return false;
        }
        if (meanTargetCoverage != null ? !meanTargetCoverage.equals(that.meanTargetCoverage) :
                that.meanTargetCoverage != null) {
            return false;
        }
        if (nearBaitBases != null ? !nearBaitBases.equals(that.nearBaitBases) : that.nearBaitBases != null) {
            return false;
        }
        if (offBaitBases != null ? !offBaitBases.equals(that.offBaitBases) : that.offBaitBases != null) {
            return false;
        }
        if (onBaitBases != null ? !onBaitBases.equals(that.onBaitBases) : that.onBaitBases != null) {
            return false;
        }
        if (onBaitVsSelected != null ? !onBaitVsSelected.equals(that.onBaitVsSelected) :
                that.onBaitVsSelected != null) {
            return false;
        }
        if (onTargetBases != null ? !onTargetBases.equals(that.onTargetBases) : that.onTargetBases != null) {
            return false;
        }
        if (pctOffBait != null ? !pctOffBait.equals(that.pctOffBait) : that.pctOffBait != null) {
            return false;
        }
        if (pctPfReads != null ? !pctPfReads.equals(that.pctPfReads) : that.pctPfReads != null) {
            return false;
        }
        if (pctPfUqReads != null ? !pctPfUqReads.equals(that.pctPfUqReads) : that.pctPfUqReads != null) {
            return false;
        }
        if (pctPfUqReadsAligned != null ? !pctPfUqReadsAligned.equals(that.pctPfUqReadsAligned) :
                that.pctPfUqReadsAligned != null) {
            return false;
        }
        if (pctSelectedBases != null ? !pctSelectedBases.equals(that.pctSelectedBases) :
                that.pctSelectedBases != null) {
            return false;
        }
        if (pctTargetBases100X != null ? !pctTargetBases100X.equals(that.pctTargetBases100X) :
                that.pctTargetBases100X != null) {
            return false;
        }
        if (pctTargetBases10X != null ? !pctTargetBases10X.equals(that.pctTargetBases10X) :
                that.pctTargetBases10X != null) {
            return false;
        }
        if (pctTargetBases20X != null ? !pctTargetBases20X.equals(that.pctTargetBases20X) :
                that.pctTargetBases20X != null) {
            return false;
        }
        if (pctTargetBases2X != null ? !pctTargetBases2X.equals(that.pctTargetBases2X) :
                that.pctTargetBases2X != null) {
            return false;
        }
        if (pctTargetBases30X != null ? !pctTargetBases30X.equals(that.pctTargetBases30X) :
                that.pctTargetBases30X != null) {
            return false;
        }
        if (percentTargetBases40X != null ? !percentTargetBases40X.equals(that.percentTargetBases40X) :
                that.percentTargetBases40X != null) {
            return false;
        }
        if (percentTargetBases50X != null ? !percentTargetBases50X.equals(that.percentTargetBases50X) :
                that.percentTargetBases50X != null) {
            return false;
        }
        if (pctUsableBasesOnBait != null ? !pctUsableBasesOnBait.equals(that.pctUsableBasesOnBait) :
                that.pctUsableBasesOnBait != null) {
            return false;
        }
        if (pctUsableBasesOnTarget != null ? !pctUsableBasesOnTarget.equals(that.pctUsableBasesOnTarget) :
                that.pctUsableBasesOnTarget != null) {
            return false;
        }
        if (pfReads != null ? !pfReads.equals(that.pfReads) : that.pfReads != null) {
            return false;
        }
        if (pfUniqueReads != null ? !pfUniqueReads.equals(that.pfUniqueReads) : that.pfUniqueReads != null) {
            return false;
        }
        if (pfUqBasesAligned != null ? !pfUqBasesAligned.equals(that.pfUqBasesAligned) :
                that.pfUqBasesAligned != null) {
            return false;
        }
        if (pfUqReadsAligned != null ? !pfUqReadsAligned.equals(that.pfUqReadsAligned) :
                that.pfUqReadsAligned != null) {
            return false;
        }
        if (targetTerritory != null ? !targetTerritory.equals(that.targetTerritory) : that.targetTerritory != null) {
            return false;
        }
        if (totalReads != null ? !totalReads.equals(that.totalReads) : that.totalReads != null) {
            return false;
        }
        if (zeroCvgTargetsPct != null ? !zeroCvgTargetsPct.equals(that.zeroCvgTargetsPct) :
                that.zeroCvgTargetsPct != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = aggregationId;
        result = 31 * result + (baitSet != null ? baitSet.hashCode() : 0);
        result = 31 * result + (genomeSize != null ? genomeSize.hashCode() : 0);
        result = 31 * result + (baitTerritory != null ? baitTerritory.hashCode() : 0);
        result = 31 * result + (targetTerritory != null ? targetTerritory.hashCode() : 0);
        result = 31 * result + (baitDesignEfficiency != null ? baitDesignEfficiency.hashCode() : 0);
        result = 31 * result + (totalReads != null ? totalReads.hashCode() : 0);
        result = 31 * result + (pfReads != null ? pfReads.hashCode() : 0);
        result = 31 * result + (pfUniqueReads != null ? pfUniqueReads.hashCode() : 0);
        result = 31 * result + (pctPfReads != null ? pctPfReads.hashCode() : 0);
        result = 31 * result + (pctPfUqReads != null ? pctPfUqReads.hashCode() : 0);
        result = 31 * result + (pfUqReadsAligned != null ? pfUqReadsAligned.hashCode() : 0);
        result = 31 * result + (pctPfUqReadsAligned != null ? pctPfUqReadsAligned.hashCode() : 0);
        result = 31 * result + (pfUqBasesAligned != null ? pfUqBasesAligned.hashCode() : 0);
        result = 31 * result + (onBaitBases != null ? onBaitBases.hashCode() : 0);
        result = 31 * result + (nearBaitBases != null ? nearBaitBases.hashCode() : 0);
        result = 31 * result + (offBaitBases != null ? offBaitBases.hashCode() : 0);
        result = 31 * result + (onTargetBases != null ? onTargetBases.hashCode() : 0);
        result = 31 * result + (pctSelectedBases != null ? pctSelectedBases.hashCode() : 0);
        result = 31 * result + (pctOffBait != null ? pctOffBait.hashCode() : 0);
        result = 31 * result + (onBaitVsSelected != null ? onBaitVsSelected.hashCode() : 0);
        result = 31 * result + (meanBaitCoverage != null ? meanBaitCoverage.hashCode() : 0);
        result = 31 * result + (meanTargetCoverage != null ? meanTargetCoverage.hashCode() : 0);
        result = 31 * result + (pctUsableBasesOnBait != null ? pctUsableBasesOnBait.hashCode() : 0);
        result = 31 * result + (pctUsableBasesOnTarget != null ? pctUsableBasesOnTarget.hashCode() : 0);
        result = 31 * result + (foldEnrichment != null ? foldEnrichment.hashCode() : 0);
        result = 31 * result + (zeroCvgTargetsPct != null ? zeroCvgTargetsPct.hashCode() : 0);
        result = 31 * result + (fold80BasePenalty != null ? fold80BasePenalty.hashCode() : 0);
        result = 31 * result + (pctTargetBases2X != null ? pctTargetBases2X.hashCode() : 0);
        result = 31 * result + (pctTargetBases10X != null ? pctTargetBases10X.hashCode() : 0);
        result = 31 * result + (pctTargetBases20X != null ? pctTargetBases20X.hashCode() : 0);
        result = 31 * result + (pctTargetBases30X != null ? pctTargetBases30X.hashCode() : 0);
        result = 31 * result + (hsLibrarySize != null ? hsLibrarySize.hashCode() : 0);
        result = 31 * result + (hsPenalty10X != null ? hsPenalty10X.hashCode() : 0);
        result = 31 * result + (hsPenalty20X != null ? hsPenalty20X.hashCode() : 0);
        result = 31 * result + (hsPenalty30X != null ? hsPenalty30X.hashCode() : 0);
        result = 31 * result + (atDropout != null ? atDropout.hashCode() : 0);
        result = 31 * result + (gcDropout != null ? gcDropout.hashCode() : 0);
        result = 31 * result + (hsPenalty40X != null ? hsPenalty40X.hashCode() : 0);
        result = 31 * result + (hsPenalty50X != null ? hsPenalty50X.hashCode() : 0);
        result = 31 * result + (hsPenalty100X != null ? hsPenalty100X.hashCode() : 0);
        result = 31 * result + (percentTargetBases40X != null ? percentTargetBases40X.hashCode() : 0);
        result = 31 * result + (percentTargetBases50X != null ? percentTargetBases50X.hashCode() : 0);
        result = 31 * result + (pctTargetBases100X != null ? pctTargetBases100X.hashCode() : 0);
        return result;
    }
}
