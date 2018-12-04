package org.broadinstitute.gpinformatics.mercury.boundary.run;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * JAXB bean used by FingerprintResource.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class FingerprintCallsBean {
    private String rsid;
    private String genotype;
    private String callConfidence;
//    private String gq;
//    private String pl;

    public FingerprintCallsBean() {
    }

    public FingerprintCallsBean(String rsid, String genotype, String callConfidence) {
        this.rsid = rsid;
        this.genotype = genotype;
        this.callConfidence = callConfidence;
    }

    public String getRsid() {
        return rsid;
    }

    public String getGenotype() {
        return genotype;
    }

    public String getCallConfidence() {
        return callConfidence;
    }

}
