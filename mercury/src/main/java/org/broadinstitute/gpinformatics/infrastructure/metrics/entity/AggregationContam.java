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

@Entity
@Table(name = "AGGREGATION_CONTAM", schema = "METRICS")
public class AggregationContam implements Serializable {
    @Id
    @Column(name = "AGGREGATION_ID", nullable = false, insertable = false, updatable = false)
    private Integer aggregationId;
    @Column(name = "SAMPLE_ALIAS")
    private String sampleAlias;
    @Column(name = "NUM_SNPS")
    private Integer numSnps;
    @Column(name = "NUM_READS")
    private Integer numReads;
    @Column(name = "MEAN_DEPTH")
    private Double meanDepth;
    @Column(name = "PCT_CONTAMINATION")
    private Double pctContamination;
    @Column(name = "LL_PREDICTED_CONTAM")
    private Double llPredictedContam;
    @Column(name = "ll_no_contam")
    private Double llNoContam;
    @OneToOne
    @JoinColumn(name = "AGGREGATION_ID", referencedColumnName = "ID", nullable = false)
    private Aggregation aggregation;

    public void setPctContamination(Double pctContamination) {
        this.pctContamination = pctContamination;
    }

    public int getAggregationId() {
        return aggregationId;
    }

    public String getSampleAlias() {
        return sampleAlias;
    }

    public Integer getNumSnps() {
        return numSnps;
    }

    public Integer getNumReads() {
        return numReads;
    }

    public Double getMeanDepth() {
        return meanDepth;
    }

    public Double getPctContamination() {
        return pctContamination;
    }

    public Double getLlPredictedContam() {
        return llPredictedContam;
    }

    public Double getLlNoContam() {
        return llNoContam;
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

        AggregationContam that = (AggregationContam) o;

        if (aggregationId != that.aggregationId) {
            return false;
        }
        if (llNoContam != null ? !llNoContam.equals(that.llNoContam) : that.llNoContam != null) {
            return false;
        }
        if (llPredictedContam != null ? !llPredictedContam.equals(that.llPredictedContam) :
                that.llPredictedContam != null) {
            return false;
        }
        if (meanDepth != null ? !meanDepth.equals(that.meanDepth) : that.meanDepth != null) {
            return false;
        }
        if (numReads != null ? !numReads.equals(that.numReads) : that.numReads != null) {
            return false;
        }
        if (numSnps != null ? !numSnps.equals(that.numSnps) : that.numSnps != null) {
            return false;
        }
        if (pctContamination != null ? !pctContamination.equals(that.pctContamination) :
                that.pctContamination != null) {
            return false;
        }
        if (sampleAlias != null ? !sampleAlias.equals(that.sampleAlias) : that.sampleAlias != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = aggregationId;
        result = 31 * result + (sampleAlias != null ? sampleAlias.hashCode() : 0);
        result = 31 * result + (numSnps != null ? numSnps.hashCode() : 0);
        result = 31 * result + (numReads != null ? numReads.hashCode() : 0);
        result = 31 * result + (meanDepth != null ? meanDepth.hashCode() : 0);
        result = 31 * result + (pctContamination != null ? pctContamination.hashCode() : 0);
        result = 31 * result + (llPredictedContam != null ? llPredictedContam.hashCode() : 0);
        result = 31 * result + (llNoContam != null ? llNoContam.hashCode() : 0);
        return result;
    }
}
