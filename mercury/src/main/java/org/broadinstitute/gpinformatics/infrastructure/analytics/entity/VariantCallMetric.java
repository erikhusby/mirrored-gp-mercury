package org.broadinstitute.gpinformatics.infrastructure.analytics.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(schema = "MERCURY", name = "VARIANT_CALL_METRICS")
public class VariantCallMetric {
    @Id
    private Long vcCallMetricsId;

    private String runName;

    private Date runDate;

    private String metricType;

    private String sampleAlias;

    private String total;

    private BigDecimal biallelic;

    private BigDecimal multiallelic;

    private BigDecimal snps;

    private BigDecimal indels;

    private BigDecimal mnps;

    @Column(name = "CHR_X_NUMBER_OF_SNPS")
    private BigDecimal chrXNumberOfSnps;

    @Column(name = "CHR_Y_NUMBER_OF_SNPS")
    private BigDecimal chrYNumberOfSnps;

    private BigDecimal snpTransitions;

    private BigDecimal snpTranversions;

    private BigDecimal heterozygous;

    private BigDecimal homozygous;

    @Column(name = "IN_DBSNP")
    private BigDecimal inDbSnp;

    @Column(name = "NOT_IN_DBSNP")
    private BigDecimal notInDbSnp;

    public Long getVcCallMetricsId() {
        return vcCallMetricsId;
    }

    public void setVcCallMetricsId(Long vcCallMetricsId) {
        this.vcCallMetricsId = vcCallMetricsId;
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

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public String getSampleAlias() {
        return sampleAlias;
    }

    public void setSampleAlias(String sampleAlias) {
        this.sampleAlias = sampleAlias;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public BigDecimal getBiallelic() {
        return biallelic;
    }

    public void setBiallelic(BigDecimal biallelic) {
        this.biallelic = biallelic;
    }

    public BigDecimal getMultiallelic() {
        return multiallelic;
    }

    public void setMultiallelic(BigDecimal multiallelic) {
        this.multiallelic = multiallelic;
    }

    public BigDecimal getSnps() {
        return snps;
    }

    public void setSnps(BigDecimal snps) {
        this.snps = snps;
    }

    public BigDecimal getIndels() {
        return indels;
    }

    public void setIndels(BigDecimal indels) {
        this.indels = indels;
    }

    public BigDecimal getMnps() {
        return mnps;
    }

    public void setMnps(BigDecimal mnps) {
        this.mnps = mnps;
    }

    public BigDecimal getChrXNumberOfSnps() {
        return chrXNumberOfSnps;
    }

    public void setChrXNumberOfSnps(BigDecimal chrXNumberOfSnps) {
        this.chrXNumberOfSnps = chrXNumberOfSnps;
    }

    public BigDecimal getChrYNumberOfSnps() {
        return chrYNumberOfSnps;
    }

    public void setChrYNumberOfSnps(BigDecimal chrYNumberOfSnps) {
        this.chrYNumberOfSnps = chrYNumberOfSnps;
    }

    public BigDecimal getSnpTransitions() {
        return snpTransitions;
    }

    public void setSnpTransitions(BigDecimal snpTransitions) {
        this.snpTransitions = snpTransitions;
    }

    public BigDecimal getSnpTranversions() {
        return snpTranversions;
    }

    public void setSnpTranversions(BigDecimal snpTransversions) {
        this.snpTranversions = snpTransversions;
    }

    public BigDecimal getHeterozygous() {
        return heterozygous;
    }

    public void setHeterozygous(BigDecimal heterozygous) {
        this.heterozygous = heterozygous;
    }

    public BigDecimal getHomozygous() {
        return homozygous;
    }

    public void setHomozygous(BigDecimal homozygous) {
        this.homozygous = homozygous;
    }

    public BigDecimal getInDbSnp() {
        return inDbSnp;
    }

    public void setInDbSnp(BigDecimal inDbSnp) {
        this.inDbSnp = inDbSnp;
    }

    public BigDecimal getNotInDbSnp() {
        return notInDbSnp;
    }

    public void setNotInDbSnp(BigDecimal notInDbSnp) {
        this.notInDbSnp = notInDbSnp;
    }
}
