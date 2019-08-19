package org.broadinstitute.gpinformatics.mercury.control.hsa.metrics;

import com.opencsv.bean.CsvBindByName;

import java.math.BigDecimal;

public class DemultiplexStats {

    @CsvBindByName(column = "Lane", required = true)
    private int lane;

    @CsvBindByName(column = "SampleID", required = true)
    private String sampleID;

    @CsvBindByName(column = "Index", required = true)
    private String index;

    @CsvBindByName(column = "# Reads", required = true)
    private long numberOfReads;

    @CsvBindByName(column = "# Perfect Index Reads", required = true)
    private long numberOfPerfectIndexReads;

    @CsvBindByName(column = "# One Mismatch Index Reads", required = true)
    private long numberOfOneMismatchIndexreads;

    @CsvBindByName(column = "# of >= Q30 Bases (PF)", required = true)
    private BigDecimal numberOfQ30BasesPassingFilter;

    @CsvBindByName(column = "Mean Quality Score (PF)", required = true)
    private String meanQualityScorePassingFilter;

    public DemultiplexStats() {
    }

    public int getLane() {
        return lane;
    }

    public void setLane(int lane) {
        this.lane = lane;
    }

    public String getSampleID() {
        return sampleID;
    }

    public void setSampleID(String sampleID) {
        this.sampleID = sampleID;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public long getNumberOfReads() {
        return numberOfReads;
    }

    public void setNumberOfReads(long numberOfReads) {
        this.numberOfReads = numberOfReads;
    }

    public long getNumberOfPerfectIndexReads() {
        return numberOfPerfectIndexReads;
    }

    public void setNumberOfPerfectIndexReads(long numberOfPerfectIndexReads) {
        this.numberOfPerfectIndexReads = numberOfPerfectIndexReads;
    }

    public long getNumberOfOneMismatchIndexreads() {
        return numberOfOneMismatchIndexreads;
    }

    public void setNumberOfOneMismatchIndexreads(long numberOfOneMismatchIndexreads) {
        this.numberOfOneMismatchIndexreads = numberOfOneMismatchIndexreads;
    }

    public BigDecimal getNumberOfQ30BasesPassingFilter() {
        return numberOfQ30BasesPassingFilter;
    }

    public void setNumberOfQ30BasesPassingFilter(BigDecimal numberOfQ30BasesPassingFilter) {
        this.numberOfQ30BasesPassingFilter = numberOfQ30BasesPassingFilter;
    }

    public String getMeanQualityScorePassingFilter() {
        return meanQualityScorePassingFilter;
    }

    public void setMeanQualityScorePassingFilter(String meanQualityScorePassingFilter) {
        this.meanQualityScorePassingFilter = meanQualityScorePassingFilter;
    }
}
