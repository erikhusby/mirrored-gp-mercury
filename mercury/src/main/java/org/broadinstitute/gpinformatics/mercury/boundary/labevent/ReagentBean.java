package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

import org.broadinstitute.gpinformatics.mercury.boundary.Namespaces;

import javax.xml.bind.annotation.XmlType;
import java.util.Date;

/**
 * JAX-RS DTO to represent a reagent in a LabEvent.
 */
@XmlType(namespace = Namespaces.LAB_EVENT)
public class ReagentBean {
    private String kitType;
    private String lotBarcode;
    private Date expiration;

    public ReagentBean(String kitType, String lotBarcode, Date expiration) {
        this.kitType = kitType;
        this.lotBarcode = lotBarcode;
        this.expiration = expiration;
    }

    /** For JAXB */
    public ReagentBean() {
    }

    public String getKitType() {
        return kitType;
    }

    public void setKitType(String kitType) {
        this.kitType = kitType;
    }

    public String getLotBarcode() {
        return lotBarcode;
    }

    public void setLotBarcode(String lotBarcode) {
        this.lotBarcode = lotBarcode;
    }

    public Date getExpiration() {
        return expiration;
    }

    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }
}
