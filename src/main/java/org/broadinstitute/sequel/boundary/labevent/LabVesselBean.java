package org.broadinstitute.sequel.boundary.labevent;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashMap;
import java.util.Map;

/**
 * A JAX-RS DTO for lab vessels, used in transfers
 */
@XmlRootElement
public class LabVesselBean {
    private String barcode;
    private String type;
    private String starter;
    private String starterType;
    private Map<String, LabVesselBean> mapPositionToLabVessel = new HashMap<String, LabVesselBean>();

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

    public Map<String, LabVesselBean> getMapPositionToLabVessel() {
        return mapPositionToLabVessel;
    }

    public void setMapPositionToLabVessel(Map<String, LabVesselBean> mapPositionToLabVessel) {
        this.mapPositionToLabVessel = mapPositionToLabVessel;
    }
}
