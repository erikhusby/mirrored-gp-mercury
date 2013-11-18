package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ParentVesselBean;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Data for adding samples to a PDO
 */
@XmlRootElement
public class AddSamplesToPdoBean {
    private String pdo;
    private String username;
    private ParentVesselBean parentVesselBean;

    public AddSamplesToPdoBean(String pdo, ParentVesselBean parentVesselBean) {
        this.pdo = pdo;
        this.parentVesselBean = parentVesselBean;
    }

    /** Used by JAXB */
    @SuppressWarnings("UnusedDeclaration")
    public AddSamplesToPdoBean() {
    }

    public String getPdo() {
        return pdo;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setPdo(String pdo) {
        this.pdo = pdo;
    }

    public String getUsername() {
        return username;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setUsername(String username) {
        this.username = username;
    }

    public ParentVesselBean getParentVesselBean() {
        return parentVesselBean;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setParentVesselBeans(ParentVesselBean parentVesselBean) {
        this.parentVesselBean = parentVesselBean;
    }
}
