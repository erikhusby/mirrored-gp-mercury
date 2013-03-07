package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.mercury.boundary.Namespaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Date;
import java.util.List;

/**
 * JAX-RS DTO for importing samples from other systems, e.g. BSP
 */
@XmlRootElement(namespace = Namespaces.VESSEL)
@XmlType(namespace = Namespaces.VESSEL)
@XmlAccessorType(XmlAccessType.FIELD)
public class SampleImportBean {
    private String sourceSystem;
    private String sourceSystemExportId;
    private Date exportDate;
    private List<ParentVesselBean> parentVesselBeans;
    private String userName;

    /** For JAXB */
    @SuppressWarnings("UnusedDeclaration")
    public SampleImportBean() {
    }

    public SampleImportBean(String sourceSystem, String sourceSystemExportId, Date exportDate,
            List<ParentVesselBean> parentVesselBeans, String userName) {
        this.sourceSystem = sourceSystem;
        this.sourceSystemExportId = sourceSystemExportId;
        this.exportDate = exportDate;
        this.parentVesselBeans = parentVesselBeans;
        this.userName = userName;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public String getSourceSystemExportId() {
        return sourceSystemExportId;
    }

    public Date getExportDate() {
        return exportDate;
    }

    public List<ParentVesselBean> getParentVesselBeans() {
        return parentVesselBeans;
    }

    public String getUserName() {
        return userName;
    }
}
