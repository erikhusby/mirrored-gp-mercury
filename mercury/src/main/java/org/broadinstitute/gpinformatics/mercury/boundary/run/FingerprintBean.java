package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;

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

    public String getDisposition() {
        return disposition;
    }

    public String getAliquotLsid() {
        return aliquotLsid;
    }

    public String getPlatform() {
        return platform;
    }

    public String getGenomeBuild() {
        return genomeBuild;
    }

    public String getSnpListName() {
        return snpListName;
    }

    public Date getDateGenerated() {
        return dateGenerated;
    }

    public String getGender() {
        return gender;
    }

    public List<FingerprintCallsBean> getCalls() {
        return calls;
    }
}
