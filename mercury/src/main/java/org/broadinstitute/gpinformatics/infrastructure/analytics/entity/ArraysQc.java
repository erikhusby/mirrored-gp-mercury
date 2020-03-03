package org.broadinstitute.gpinformatics.infrastructure.analytics.entity;

import org.hibernate.annotations.BatchSize;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;

/**
 * Entity for array chip well metrics, ETL'd from cloud database by Analytics team.
 */
@Entity
@Table(schema = "ANALYTICS")
public class ArraysQc {
    @Id
    private Long id;
    private String chipWellBarcode;
    private String sampleAlias;
    private Long analysisVersion;
    private Boolean isLatest;
    private String chipType;
    /** zCall call rate*/
    private BigDecimal callRate;
    private BigDecimal autocallCallRate;
    private Long numAutocallCalls;
    private Boolean autocallPf;
    private Date autocallDate;
    private Date imagingDate;
    private Boolean isZcalled;
    private Character autocallGender;
    private Character fpGender;
    private Character reportedGender;
    // See GPLIM-4863, this column needs 3 states so needs to be replaced with business logic until analytics changes
    private Boolean genderConcordancePf;
    private BigDecimal hetPct;
    private BigDecimal hetHomvarRatio;
    private String clusterFileName;
    @Column(name = "P95_GREEN")
    private Long p95Green;
    @Column(name = "P95_RED")
    private Long p95Red;
    private String autocallVersion;
    private String zcallVersion;
    private Long totalAssays;
    private Long totalSnps;
    private Long totalIndels;
    private Long numCalls;
    private Long numNoCalls;
    private Long numInDbSnp;
    private Long novelSnps;
    private Long filteredSnps;
    private BigDecimal pctDbsnp;
    private Long numSingletons;
    private Date createdAt;
    private Date modifiedAt;

    @OneToMany(mappedBy = "arraysQcId")
    @BatchSize(size = 100)
    private Set<ArraysQcFingerprint> arraysQcFingerprints;

    @OneToMany(mappedBy = "arraysQcId")
    @BatchSize(size = 100)
    private Set<ArraysQcGtConcordance> arraysQcGtConcordances;

    /**
     * This is actually OneToOne, see getArraysQcContamination() <br/>
     * Mapped as OneToMany to avoid a separate query for each ArraysQc entity
     */
    @OneToMany(mappedBy = "arraysQcId")
    @BatchSize(size = 100)
    private Set<ArraysQcContamination> arraysQcContamination;

    public Long getId() {
        return id;
    }

    public String getChipWellBarcode() {
        return chipWellBarcode;
    }

    public String getSampleAlias() {
        return sampleAlias;
    }

    public Long getAnalysisVersion() {
        return analysisVersion;
    }

    public Boolean getLatest() {
        return isLatest;
    }

    public String getChipType() {
        return chipType;
    }

    public BigDecimal getCallRate() {
        return callRate;
    }

    public BigDecimal getCallRatePct() {
        if (callRate == null) {
            return null;
        }
        return callRate.multiply(BigDecimal.valueOf(100)).setScale(3, BigDecimal.ROUND_HALF_UP);
    }

    public BigDecimal getAutocallCallRate() {
        return autocallCallRate;
    }

    public BigDecimal getAutocallCallRatePct() {
        if (autocallCallRate == null) {
            return null;
        }
        return autocallCallRate.multiply(BigDecimal.valueOf(100)).setScale(3, BigDecimal.ROUND_HALF_UP);
    }

    public Long getNumAutocallCalls() {
        return numAutocallCalls;
    }

    public Boolean getAutocallPf() {
        return autocallPf;
    }

    public Date getAutocallDate() {
        return autocallDate;
    }

    public Date getImagingDate() {
        return imagingDate;
    }

    public Boolean getZcalled() {
        return isZcalled;
    }

    public Character getAutocallGender() {
        return autocallGender==null?Character.MIN_VALUE:autocallGender;
    }

    public Character getFpGender() {
        return fpGender==null?Character.MIN_VALUE:fpGender;
    }

    public Character getReportedGender() {
        return reportedGender==null?Character.MIN_VALUE:reportedGender;
    }

    /**
     * Display logic for potential states  See GPLIM-4863, GPLIM-6134
     */
    public String getGenderConcordancePf() {
        // The existing 0.2% nulls in this column would be reported as N/A
        if( genderConcordancePf == null ) {
            return "N/A";
        }

        // Low hanging fruit:  If analytics says it passes, it passes
        // Correlates to GPLIM-6134 including 2 matches with an unreported are a Pass
        if( genderConcordancePf ) {
            return "Pass";
        }

        // Data cleanse - replace the possible nulls with spaces
        Character theReportedGender = getReportedGender();
        // Convert ~40 'N' values to 'U'
        if( theReportedGender.equals('N')) {
            theReportedGender = Character.valueOf('U');
        }
        Character theAutocallGender = getAutocallGender();
        Character theFpGender = getFpGender();

        // 2 Unreporteds should show as N/A (overwrites DB Fail value)
        if( theReportedGender.equals('U') && theFpGender.equals('U') ) {
            return "N/A";
        }

        // Handle the thousand or so nulls (overwrites DB NULL value)
        if( theFpGender.equals(Character.MIN_VALUE) || theAutocallGender.equals(Character.MIN_VALUE) ) {
            return "N/A";
        }

        // Default is Fail
        return "Fail";
    }

    public BigDecimal getHetPct() {
        return hetPct;
    }

    public BigDecimal getHetPct100() {
        if (hetPct == null) {
            return null;
        }
        return hetPct.multiply(BigDecimal.valueOf(100)).setScale(3, BigDecimal.ROUND_HALF_UP);
    }

    public BigDecimal getHetHomvarRatio() {
        return hetHomvarRatio;
    }

    public String getClusterFileName() {
        return clusterFileName;
    }

    public Long getP95Green() {
        return p95Green;
    }

    public Long getP95Red() {
        return p95Red;
    }

    public String getAutocallVersion() {
        return autocallVersion;
    }

    public String getZcallVersion() {
        return zcallVersion;
    }

    public Long getTotalAssays() {
        return totalAssays;
    }

    public Long getTotalSnps() {
        return totalSnps;
    }

    public Long getTotalIndels() {
        return totalIndels;
    }

    public Long getNumCalls() {
        return numCalls;
    }

    public Long getNumNoCalls() {
        return numNoCalls;
    }

    public Long getNumInDbSnp() {
        return numInDbSnp;
    }

    public Long getNovelSnps() {
        return novelSnps;
    }

    public Long getFilteredSnps() {
        return filteredSnps;
    }

    public BigDecimal getPctDbsnp() {
        return pctDbsnp;
    }

    public Long getNumSingletons() {
        return numSingletons;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getModifiedAt() {
        return modifiedAt;
    }

    public Set<ArraysQcFingerprint> getArraysQcFingerprints() {
        return arraysQcFingerprints;
    }

    public Set<ArraysQcGtConcordance> getArraysQcGtConcordances() {
        return arraysQcGtConcordances;
    }

    /**
     * This is OneToOne, mapped as OneToMany to avoid separate query for each ArraysQc entity <br/>
     * As of 01/2020, PK = ANALYTICS.ARRAYS_QC_CONTAMINATION(ARRAYS_QC_ID)
     */
    public ArraysQcContamination getArraysQcContamination() {
        if (arraysQcContamination == null || arraysQcContamination.size() == 0) {
            return null;
        }
        return arraysQcContamination.iterator().next();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setChipWellBarcode(String chipWellBarcode) {
        this.chipWellBarcode = chipWellBarcode;
    }

    public void setSampleAlias(String sampleAlias) {
        this.sampleAlias = sampleAlias;
    }

    public void setAnalysisVersion(Long analysisVersion) {
        this.analysisVersion = analysisVersion;
    }

    public void setLatest(Boolean latest) {
        isLatest = latest;
    }

    public void setChipType(String chipType) {
        this.chipType = chipType;
    }

    public void setCallRate(BigDecimal callRate) {
        this.callRate = callRate;
    }

    public void setAutocallCallRate(BigDecimal autocallCallRate) {
        this.autocallCallRate = autocallCallRate;
    }

    public void setNumAutocallCalls(Long numAutocallCalls) {
        this.numAutocallCalls = numAutocallCalls;
    }

    public void setAutocallPf(Boolean autocallPf) {
        this.autocallPf = autocallPf;
    }

    public void setAutocallDate(Date autocallDate) {
        this.autocallDate = autocallDate;
    }

    public void setImagingDate(Date imagingDate) {
        this.imagingDate = imagingDate;
    }

    public void setZcalled(Boolean zcalled) {
        isZcalled = zcalled;
    }

    public void setAutocallGender(Character autocallGender) {
        this.autocallGender = autocallGender;
    }

    public void setFpGender(Character fpGender) {
        this.fpGender = fpGender;
    }

    public void setReportedGender(Character reportedGender) {
        this.reportedGender = reportedGender;
    }

    public void setGenderConcordancePf(Boolean genderConcordancePf) {
        this.genderConcordancePf = genderConcordancePf;
    }

    public void setHetPct(BigDecimal hetPct) {
        this.hetPct = hetPct;
    }

    public void setHetHomvarRatio(BigDecimal hetHomvarRatio) {
        this.hetHomvarRatio = hetHomvarRatio;
    }

    public void setClusterFileName(String clusterFileName) {
        this.clusterFileName = clusterFileName;
    }

    public void setP95Green(Long p95Green) {
        this.p95Green = p95Green;
    }

    public void setP95Red(Long p95Red) {
        this.p95Red = p95Red;
    }

    public void setAutocallVersion(String autocallVersion) {
        this.autocallVersion = autocallVersion;
    }

    public void setZcallVersion(String zcallVersion) {
        this.zcallVersion = zcallVersion;
    }

    public void setTotalAssays(Long totalAssays) {
        this.totalAssays = totalAssays;
    }

    public void setTotalSnps(Long totalSnps) {
        this.totalSnps = totalSnps;
    }

    public void setTotalIndels(Long totalIndels) {
        this.totalIndels = totalIndels;
    }

    public void setNumCalls(Long numCalls) {
        this.numCalls = numCalls;
    }

    public void setNumNoCalls(Long numNoCalls) {
        this.numNoCalls = numNoCalls;
    }

    public void setNumInDbSnp(Long numInDbSnp) {
        this.numInDbSnp = numInDbSnp;
    }

    public void setNovelSnps(Long novelSnps) {
        this.novelSnps = novelSnps;
    }

    public void setFilteredSnps(Long filteredSnps) {
        this.filteredSnps = filteredSnps;
    }

    public void setPctDbsnp(BigDecimal pctDbsnp) {
        this.pctDbsnp = pctDbsnp;
    }

    public void setNumSingletons(Long numSingletons) {
        this.numSingletons = numSingletons;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void setModifiedAt(Date modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public void setArraysQcFingerprints(
            Set<ArraysQcFingerprint> arraysQcFingerprints) {
        this.arraysQcFingerprints = arraysQcFingerprints;
    }

    public void setArraysQcGtConcordances(
            Set<ArraysQcGtConcordance> arraysQcGtConcordances) {
        this.arraysQcGtConcordances = arraysQcGtConcordances;
    }

    public void setArraysQcContamination(
            Set<ArraysQcContamination> arraysQcContamination) {
        this.arraysQcContamination = arraysQcContamination;
    }
}