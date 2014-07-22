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
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "AGGREGATION_ALIGNMENT", schema = "METRICS")
public class AggregationAlignment implements Serializable {
    @Id
    @Column(name = "AGGREGATION_ID", nullable = false, insertable = false, updatable = false)
    private int aggregationId;
    @Id
    private String category;
    @Column(name = "total_reads") private Integer totalReads;
    @Column(name = "PF_READS") private Integer pfReads;
    @Column(name = "PCT_PF_READS") private Double pctPfReads;
    @Column(name = "PF_NOISE_READS") private Integer pfNoiseReads;
    @Column(name = "PF_READS_ALIGNED") private Integer pfReadsAligned;
    @Column(name = "PCT_PF_READS_ALIGNED") private Double pctPfReadsAligned;
    @Column(name = "PF_ALIGNED_BASES") private Integer pfAlignedBases;
    @Column(name = "PF_HQ_ALIGNED_READS") private Integer pfHqAlignedReads;
    @Column(name = "PF_HQ_ALIGNED_BASES") private Integer pfHqAlignedBases;
    @Column(name = "PF_HQ_ALIGNED_Q20_BASES") private Integer pfHqAlignedQ20Bases;
    @Column(name = "PF_HQ_MEDIAN_MISMATCHES") private Integer pfHqMedianMismatches;
    @Column(name = "PF_MISMATCH_RATE") private Double pfMismatchRate;
    @Column(name = "PF_HQ_ERROR_RATE") private Double pfHqErrorRate;
    @Column(name = "PF_INDEL_RATE") private Double pfIndelRate;
    @Column(name = "MEAN_READ_LENGTH") private Integer meanReadLength;
    @Column(name = "READS_ALIGNED_IN_PAIRS") private Integer readsAlignedInPairs;
    @Column(name = "PCT_READS_ALIGNED_IN_PAIRS") private Double pctReadsAlignedInPairs;
    @Column(name = "BAD_CYCLES") private Integer badCycles;
    @Column(name = "STRAND_BALANCE") private Double strandBalance;
    @Column(name = "PCT_CHIMERAS") private Double pctChimeras;
    @Column(name = "PCT_ADAPTER") private Double pctAdapter;
    @ManyToOne
    @JoinColumn(name = "AGGREGATION_ID", referencedColumnName = "ID", nullable = false)
    private Aggregation aggregation;

    /**
     * Default constructor for JPA.
     */
    protected AggregationAlignment() {}

    public AggregationAlignment(Integer pfReadsAligned) {
        this.pfReadsAligned = pfReadsAligned;
    }

    public String getCategory() {
        return category;
    }

    public Integer getTotalReads() {
        return totalReads;
    }

    public Integer getPfReads() {
        return pfReads;
    }

    public Double getPctPfReads() {
        return pctPfReads;
    }

    public Integer getPfNoiseReads() {
        return pfNoiseReads;
    }

    public Integer getPfReadsAligned() {
        return pfReadsAligned;
    }

    public Double getPctPfReadsAligned() {
        return pctPfReadsAligned;
    }

    public Integer getPfAlignedBases() {
        return pfAlignedBases;
    }

    public Integer getPfHqAlignedReads() {
        return pfHqAlignedReads;
    }

    public Integer getPfHqAlignedBases() {
        return pfHqAlignedBases;
    }

    public Integer getPfHqAlignedQ20Bases() {
        return pfHqAlignedQ20Bases;
    }

    public Integer getPfHqMedianMismatches() {
        return pfHqMedianMismatches;
    }

    public Double getPfMismatchRate() {
        return pfMismatchRate;
    }

    public Double getPfHqErrorRate() {
        return pfHqErrorRate;
    }

    public Double getPfIndelRate() {
        return pfIndelRate;
    }

    public Integer getMeanReadLength() {
        return meanReadLength;
    }

    public Integer getReadsAlignedInPairs() {
        return readsAlignedInPairs;
    }

    public Double getPctReadsAlignedInPairs() {
        return pctReadsAlignedInPairs;
    }

    public Integer getBadCycles() {
        return badCycles;
    }

    public Double getStrandBalance() {
        return strandBalance;
    }

    public Double getPctChimeras() {
        return pctChimeras;
    }

    public Double getPctAdapter() {
        return pctAdapter;
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

        AggregationAlignment that = (AggregationAlignment) o;

        if (aggregationId != that.aggregationId) {
            return false;
        }
        if (badCycles != null ? !badCycles.equals(that.badCycles) : that.badCycles != null) {
            return false;
        }
        if (category != null ? !category.equals(that.category) : that.category != null) {
            return false;
        }
        if (meanReadLength != null ? !meanReadLength.equals(that.meanReadLength) : that.meanReadLength != null) {
            return false;
        }
        if (pctAdapter != null ? !pctAdapter.equals(that.pctAdapter) : that.pctAdapter != null) {
            return false;
        }
        if (pctChimeras != null ? !pctChimeras.equals(that.pctChimeras) : that.pctChimeras != null) {
            return false;
        }
        if (pctPfReads != null ? !pctPfReads.equals(that.pctPfReads) : that.pctPfReads != null) {
            return false;
        }
        if (pctPfReadsAligned != null ? !pctPfReadsAligned.equals(that.pctPfReadsAligned) :
                that.pctPfReadsAligned != null) {
            return false;
        }
        if (pctReadsAlignedInPairs != null ? !pctReadsAlignedInPairs.equals(that.pctReadsAlignedInPairs) :
                that.pctReadsAlignedInPairs != null) {
            return false;
        }
        if (pfAlignedBases != null ? !pfAlignedBases.equals(that.pfAlignedBases) : that.pfAlignedBases != null) {
            return false;
        }
        if (pfHqAlignedBases != null ? !pfHqAlignedBases.equals(that.pfHqAlignedBases) :
                that.pfHqAlignedBases != null) {
            return false;
        }
        if (pfHqAlignedQ20Bases != null ? !pfHqAlignedQ20Bases.equals(that.pfHqAlignedQ20Bases) :
                that.pfHqAlignedQ20Bases != null) {
            return false;
        }
        if (pfHqAlignedReads != null ? !pfHqAlignedReads.equals(that.pfHqAlignedReads) :
                that.pfHqAlignedReads != null) {
            return false;
        }
        if (pfHqErrorRate != null ? !pfHqErrorRate.equals(that.pfHqErrorRate) : that.pfHqErrorRate != null) {
            return false;
        }
        if (pfHqMedianMismatches != null ? !pfHqMedianMismatches.equals(that.pfHqMedianMismatches) :
                that.pfHqMedianMismatches != null) {
            return false;
        }
        if (pfIndelRate != null ? !pfIndelRate.equals(that.pfIndelRate) : that.pfIndelRate != null) {
            return false;
        }
        if (pfMismatchRate != null ? !pfMismatchRate.equals(that.pfMismatchRate) : that.pfMismatchRate != null) {
            return false;
        }
        if (pfNoiseReads != null ? !pfNoiseReads.equals(that.pfNoiseReads) : that.pfNoiseReads != null) {
            return false;
        }
        if (pfReads != null ? !pfReads.equals(that.pfReads) : that.pfReads != null) {
            return false;
        }
        if (pfReadsAligned != null ? !pfReadsAligned.equals(that.pfReadsAligned) : that.pfReadsAligned != null) {
            return false;
        }
        if (readsAlignedInPairs != null ? !readsAlignedInPairs.equals(that.readsAlignedInPairs) :
                that.readsAlignedInPairs != null) {
            return false;
        }
        if (strandBalance != null ? !strandBalance.equals(that.strandBalance) : that.strandBalance != null) {
            return false;
        }
        if (totalReads != null ? !totalReads.equals(that.totalReads) : that.totalReads != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = aggregationId;
        result = 31 * result + (category != null ? category.hashCode() : 0);
        result = 31 * result + (totalReads != null ? totalReads.hashCode() : 0);
        result = 31 * result + (pfReads != null ? pfReads.hashCode() : 0);
        result = 31 * result + (pctPfReads != null ? pctPfReads.hashCode() : 0);
        result = 31 * result + (pfNoiseReads != null ? pfNoiseReads.hashCode() : 0);
        result = 31 * result + (pfReadsAligned != null ? pfReadsAligned.hashCode() : 0);
        result = 31 * result + (pctPfReadsAligned != null ? pctPfReadsAligned.hashCode() : 0);
        result = 31 * result + (pfAlignedBases != null ? pfAlignedBases.hashCode() : 0);
        result = 31 * result + (pfHqAlignedReads != null ? pfHqAlignedReads.hashCode() : 0);
        result = 31 * result + (pfHqAlignedBases != null ? pfHqAlignedBases.hashCode() : 0);
        result = 31 * result + (pfHqAlignedQ20Bases != null ? pfHqAlignedQ20Bases.hashCode() : 0);
        result = 31 * result + (pfHqMedianMismatches != null ? pfHqMedianMismatches.hashCode() : 0);
        result = 31 * result + (pfMismatchRate != null ? pfMismatchRate.hashCode() : 0);
        result = 31 * result + (pfHqErrorRate != null ? pfHqErrorRate.hashCode() : 0);
        result = 31 * result + (pfIndelRate != null ? pfIndelRate.hashCode() : 0);
        result = 31 * result + (meanReadLength != null ? meanReadLength.hashCode() : 0);
        result = 31 * result + (readsAlignedInPairs != null ? readsAlignedInPairs.hashCode() : 0);
        result = 31 * result + (pctReadsAlignedInPairs != null ? pctReadsAlignedInPairs.hashCode() : 0);
        result = 31 * result + (badCycles != null ? badCycles.hashCode() : 0);
        result = 31 * result + (strandBalance != null ? strandBalance.hashCode() : 0);
        result = 31 * result + (pctChimeras != null ? pctChimeras.hashCode() : 0);
        result = 31 * result + (pctAdapter != null ? pctAdapter.hashCode() : 0);
        return result;
    }

}
