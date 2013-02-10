package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.mercury.boundary.Namespaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Date;
import java.util.List;

/**
 * JAX-RS DTO used by SampleReceiptResource.
 */
@XmlRootElement(namespace = Namespaces.VESSEL)
@XmlType(namespace = Namespaces.VESSEL)
@XmlAccessorType(XmlAccessType.FIELD)
public class SampleReceiptBean {
    private Date receiptDate;
    private String kitId;
    private List<ParentVesselBean> parentVesselBeans;

    public SampleReceiptBean(Date receiptDate, String kitId, List<ParentVesselBean> parentVesselBeans) {
        this.receiptDate = receiptDate;
        this.kitId = kitId;
        this.parentVesselBeans = parentVesselBeans;
    }

    public Date getReceiptDate() {
        return receiptDate;
    }

    public String getKitId() {
        return kitId;
    }

    public List<ParentVesselBean> getParentVesselBeans() {
        return parentVesselBeans;
    }
}
