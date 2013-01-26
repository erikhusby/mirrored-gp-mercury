package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.mercury.boundary.Namespaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.math.BigDecimal;

/**
 * JAX-RS DTO to represent a quantification of a vessel, within a QuantRunBean
 */
@XmlRootElement(namespace = Namespaces.VESSEL)
@XmlType(namespace = Namespaces.VESSEL)
@XmlAccessorType(XmlAccessType.FIELD)
public class VesselMetricBean {
    private String barcode;
    private BigDecimal value;
    private boolean pass;
    private String containerPosition;

    /** For JAXB */
    public VesselMetricBean() {
    }

    public VesselMetricBean(String barcode, BigDecimal value) {
        this.barcode = barcode;
        this.value = value;
    }

    public VesselMetricBean(String barcode, BigDecimal value, boolean pass, String containerPosition) {
        this.barcode = barcode;
        this.value = value;
        this.pass = pass;
        this.containerPosition = containerPosition;
    }

    public String getBarcode() {
        return barcode;
    }

    public BigDecimal getValue() {
        return value;
    }

    public boolean isPass() {
        return pass;
    }

    public String getContainerPosition() {
        return containerPosition;
    }
}
