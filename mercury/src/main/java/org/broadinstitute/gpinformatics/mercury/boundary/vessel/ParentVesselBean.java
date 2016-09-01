package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.mercury.boundary.Namespaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * JAX-RS DTO for a parent vessel, e.g. a rack of tubes or a plate of wells.  Also used for individual tubes where
 * the rack is not known, and other vessels
 */
@XmlRootElement(namespace = Namespaces.VESSEL)
@XmlType(namespace = Namespaces.VESSEL)
@XmlAccessorType(XmlAccessType.FIELD)
public class ParentVesselBean {
    private String manufacturerBarcode;
    private String plateName;
    private String sampleId;
    private String vesselType;
    private List<ChildVesselBean> childVesselBeans;

    /** For JAXB */
    public ParentVesselBean() {
    }

    public ParentVesselBean(String manufacturerBarcode, String plateName, String sampleId, String vesselType,
            List<ChildVesselBean> childVesselBeans) {
        this(manufacturerBarcode, sampleId, vesselType, childVesselBeans);
        this.plateName = plateName;
    }

    public ParentVesselBean(String manufacturerBarcode, String sampleId, String vesselType, List<ChildVesselBean> childVesselBeans) {
        this.manufacturerBarcode = manufacturerBarcode;
        this.sampleId = sampleId;
        this.vesselType = vesselType;
        this.childVesselBeans = childVesselBeans;
    }

    public String getManufacturerBarcode() {
        return manufacturerBarcode;
    }

    public String getPlateName() {
        return plateName;
    }

    public String getSampleId() {
        return sampleId;
    }

    public String getVesselType() {
        return vesselType;
    }

    public List<ChildVesselBean> getChildVesselBeans() {
        return childVesselBeans;
    }
}
