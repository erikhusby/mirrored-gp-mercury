package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

import org.broadinstitute.gpinformatics.mercury.boundary.Namespaces;

import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * A JAX-RS DTO for lab vessels, used in transfers
 */
@XmlType(namespace = Namespaces.LAB_EVENT)
public class LabVesselBean {
    private String barcode;
    private String type;
    private String starter;
    private String starterType;
    private List<LabVesselPositionBean> labVesselPositionBeans = new java.util.ArrayList<>();

    public LabVesselBean(String barcode, String type) {
        this.barcode = barcode;
        this.type = type;
    }

    /** Used by JAXB */
    public LabVesselBean() {
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStarter() {
        return starter;
    }

    public void setStarter(String starter) {
        this.starter = starter;
    }

    public String getStarterType() {
        return starterType;
    }

    public void setStarterType(String starterType) {
        this.starterType = starterType;
    }

    public List<LabVesselPositionBean> getLabVesselPositionBeans() {
        return labVesselPositionBeans;
    }

    public void setLabVesselPositionBeans(List<LabVesselPositionBean> labVesselPositionBeans) {
        this.labVesselPositionBeans = labVesselPositionBeans;
    }
}
