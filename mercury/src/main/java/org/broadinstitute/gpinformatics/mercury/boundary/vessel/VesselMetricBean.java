package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.mercury.boundary.Namespaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * JAX-RS DTO to represent a quantification of a vessel, within a QuantRunBean
 */
@XmlRootElement(namespace = Namespaces.VESSEL)
@XmlType(namespace = Namespaces.VESSEL)
@XmlAccessorType(XmlAccessType.FIELD)
public class VesselMetricBean {
    /** Barcode of the vessel */
    private String barcode;
    /** The value of the metric.  String rather than Float to avoid IEEE representation problems */
    private String value;
    /** The unit of the value, e.g. ng/uL */
    private String unit;
    /** For qPCR, whether the value passes the criteria */
    private boolean pass;
    /** The position of the tube in the rack, e.g. A01 */
    private String containerPosition;

    /** For JAXB */
    public VesselMetricBean() {
    }

    public VesselMetricBean(String barcode, String value, String unit) {
        this.barcode = barcode;
        this.value = value;
        this.unit = unit;
    }

    public VesselMetricBean(String barcode, String value, String unit, boolean pass, String containerPosition) {
        this(barcode, value, unit);
        this.pass = pass;
        this.containerPosition = containerPosition;
    }

    public String getBarcode() {
        return barcode;
    }

    public String getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    public boolean isPass() {
        return pass;
    }

    public String getContainerPosition() {
        return containerPosition;
    }
}
