package org.broadinstitute.gpinformatics.infrastructure.analytics.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
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
    private String project;
    private String sampleAlias;
    private Long analysisVersion;
    private Boolean isLatest;
    private String chipType;
    private BigDecimal callRate;
    private Boolean autocallPf;
    private Date autocallDate;
    private Boolean isZcalled;
    private Character autocallGender;
    private Character fpGender;
    private Character reportedGender;
    private Boolean genderConcordancePf;
    private BigDecimal hetPct;
    private String clusterFile;
    @Column(name = "P95_GREEN")
    private Long p95Green;
    @Column(name = "P95_GREEN_PF")
    private Boolean p95GreenPf;
    @Column(name = "P95_RED")
    private Long p95Red;
    @Column(name = "P95_RED_PF")
    private Boolean p95RedPf;
    private String autocallVersion;
    private String zcallVersion;
    private Date createdAt;
    private Date modifiedAt;

    @OneToMany(mappedBy = "arraysQcId")
    private Set<ArraysQcFingerprint> arraysQcFingerprints;

    @OneToMany(mappedBy = "arraysQcId")
    private Set<ArraysQcGtConcordance> arraysQcGtConcordances;

    public Long getId() {
        return id;
    }

    public String getChipWellBarcode() {
        return chipWellBarcode;
    }

    public String getProject() {
        return project;
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

    public Boolean getAutocallPf() {
        return autocallPf;
    }

    public Date getAutocallDate() {
        return autocallDate;
    }

    public Boolean getZcalled() {
        return isZcalled;
    }

    public Character getAutocallGender() {
        return autocallGender;
    }

    public Character getFpGender() {
        return fpGender;
    }

    public Character getReportedGender() {
        return reportedGender;
    }

    public Boolean getGenderConcordancePf() {
        return genderConcordancePf;
    }

    public BigDecimal getHetPct() {
        return hetPct;
    }

    public String getClusterFile() {
        return clusterFile;
    }

    public Long getP95Green() {
        return p95Green;
    }

    public Boolean getP95GreenPf() {
        return p95GreenPf;
    }

    public Long getP95Red() {
        return p95Red;
    }

    public Boolean getP95RedPf() {
        return p95RedPf;
    }

    public String getAutocallVersion() {
        return autocallVersion;
    }

    public String getZcallVersion() {
        return zcallVersion;
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
}