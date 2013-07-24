package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.mercury.boundary.Namespaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * JAX-RS DTO to represent a quantification run, e.g. Pico, Eco or Viaa7
 */
@XmlRootElement(namespace = Namespaces.VESSEL)
@XmlType(namespace = Namespaces.VESSEL)
@XmlAccessorType(XmlAccessType.FIELD)
public class VesselMetricRunBean {
    private String runName;
    private Date runDate;
    private String quantType;
    private List<VesselMetricBean> vesselMetricBeans = new ArrayList<>();

    public VesselMetricRunBean() {
    }

    public VesselMetricRunBean(String runName, Date runDate, String quantType, List<VesselMetricBean> vesselMetricBeans) {
        this.runName = runName;
        this.runDate = runDate;
        this.quantType = quantType;
        this.vesselMetricBeans = vesselMetricBeans;
    }

    public String getRunName() {
        return runName;
    }

    public Date getRunDate() {
        return runDate;
    }

    public String getQuantType() {
        return quantType;
    }

    public List<VesselMetricBean> getVesselMetricBeans() {
        return vesselMetricBeans;
    }
}
