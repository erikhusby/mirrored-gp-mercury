package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.mercury.boundary.Namespaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * JAX-RS DTO for a child vessel, e.g. a tube in a rack or well in a plate
 */
@XmlRootElement(namespace = Namespaces.VESSEL)
@XmlType(namespace = Namespaces.VESSEL)
@XmlAccessorType(XmlAccessType.FIELD)
public class ChildVesselBean {
    private String manufacturerBarcode;
    private String sampleId;
    private String productOrderKey;
    private String vesselType;
    private String position;

    /** For JAXB */
    ChildVesselBean() {
    }

    public ChildVesselBean(String manufacturerBarcode, String sampleId, String productOrderKey, String vesselType, String position) {
        this.manufacturerBarcode = manufacturerBarcode;
        this.sampleId = sampleId;
        this.productOrderKey = productOrderKey;
        this.vesselType = vesselType;
        this.position = position;
    }

    public String getManufacturerBarcode() {
        return manufacturerBarcode;
    }

    public String getSampleId() {
        return sampleId;
    }

    public String getProductOrderKey() {
        return productOrderKey;
    }

    public String getVesselType() {
        return vesselType;
    }

    public String getPosition() {
        return position;
    }
}
