package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

import org.broadinstitute.gpinformatics.mercury.boundary.Namespaces;

import javax.xml.bind.annotation.XmlType;

/**
 * JAX-RS DTO to represent a reagent in a LabEvent.
 */
@XmlType(namespace = Namespaces.LAB_EVENT)
public class ReagentBean {
    private String kitType;
    private String lotBarcode;

    public ReagentBean(String kitType, String lotBarcode) {
        this.kitType = kitType;
        this.lotBarcode = lotBarcode;
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
}
