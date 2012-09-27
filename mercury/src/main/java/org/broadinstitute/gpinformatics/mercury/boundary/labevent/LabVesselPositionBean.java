package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

import org.broadinstitute.gpinformatics.mercury.boundary.Namespaces;

import javax.xml.bind.annotation.XmlType;

/**
 * JAX-RS DTO to represent position of LabVessel in a VesselContainer
 */
@XmlType(namespace = Namespaces.LAB_EVENT)
public class LabVesselPositionBean {
    private String position;
    private LabVesselBean labVesselBean;

    public LabVesselPositionBean(String position, LabVesselBean labVesselBean) {
        this.position = position;
        this.labVesselBean = labVesselBean;
    }

    /** For JAXB */
    public LabVesselPositionBean() {
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public LabVesselBean getLabVesselBean() {
        return labVesselBean;
    }

    public void setLabVesselBean(LabVesselBean labVesselBean) {
        this.labVesselBean = labVesselBean;
    }
}
