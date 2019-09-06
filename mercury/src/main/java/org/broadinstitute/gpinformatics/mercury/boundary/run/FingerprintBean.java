package org.broadinstitute.gpinformatics.mercury.boundary.run;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;
import java.util.List;

/**
 * JAXB bean used by FingerprintResource.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class FingerprintBean {
    private String queriedLsid;
    private String disposition;
    private String aliquotLsid;
    private String platform;
    private String genomeBuild;
    private String snpListName;
    @JsonDeserialize(using = CustomJsonDateDeserializer.class)
    @JsonSerialize(using = CustomJsonDateSerializer.class)
    private Date dateGenerated;
    private String gender;
    // empty if failed
    private List<FingerprintCallsBean> calls;

    /** For JAXB. */
    public FingerprintBean() {
    }

    public FingerprintBean(String queriedLsid, String disposition, String aliquotLsid, String platform,
            String genomeBuild, String snpListName, Date dateGenerated, String gender, List<FingerprintCallsBean> calls) {
        this.queriedLsid = queriedLsid;
        this.disposition = disposition;
        this.aliquotLsid = aliquotLsid;
        this.platform = platform;
        this.genomeBuild = genomeBuild;
        this.snpListName = snpListName;
        this.dateGenerated = dateGenerated;
        this.gender = gender;
        this.calls = calls;
    }

    public String getQueriedLsid() {
        return queriedLsid;
    }

    public void setQueriedLsid(String queriedLsid) {
        this.queriedLsid = queriedLsid;
    }

    public String getDisposition() {
        return disposition;
    }

    public void setDisposition(String disposition) {
        this.disposition = disposition;
    }

    public String getAliquotLsid() {
        return aliquotLsid;
    }

    public void setAliquotLsid(String aliquotLsid) {
        this.aliquotLsid = aliquotLsid;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getGenomeBuild() {
        return genomeBuild;
    }

    public void setGenomeBuild(String genomeBuild) {
        this.genomeBuild = genomeBuild;
    }

    public String getSnpListName() {
        return snpListName;
    }

    public void setSnpListName(String snpListName) {
        this.snpListName = snpListName;
    }

    public Date getDateGenerated() {
        return dateGenerated;
    }

    public void setDateGenerated(Date dateGenerated) {
        this.dateGenerated = dateGenerated;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public List<FingerprintCallsBean> getCalls() {
        return calls;
    }

    public void setCalls(List<FingerprintCallsBean> calls) {
        this.calls = calls;
    }
}
