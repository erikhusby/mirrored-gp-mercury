package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ParentVesselBean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data for adding samples to a PDO
 */
@SuppressWarnings("UnusedDeclaration")
@XmlRootElement
public class AddSamplesToPdoBean {
    private String pdo;
    private String username;

    @XmlElement(nillable = true)
    protected List<ParentVesselBean> parentVesselBeans;

    public AddSamplesToPdoBean(String username, String pdo, ParentVesselBean parentVesselBean) {
        this.pdo = pdo;
        this.username = username;
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

    /**
     * Gets the value of the parentVesselBeans property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the parentVesselBeans property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getParentVesselBeans().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ParentVesselBean }
     *
     *
     */
    public List<ParentVesselBean> getParentVesselBeans() {
        if (parentVesselBeans == null) {
            parentVesselBeans = new ArrayList<>();
    }
        return this.parentVesselBeans;
    }

    public void addSample(ParentVesselBean sample) {
        getParentVesselBeans().add(sample);
    }
}
