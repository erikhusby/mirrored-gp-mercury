package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * JAX-RS DTO to represent a quantification run, e.g. Pico, Eco or Viaa7
 */
public class VesselMetricRunBean {
    private String runName;
    private Date runDate;
    private String quantType;
    private List<VesselMetricBean> vesselMetricBeans = new ArrayList<VesselMetricBean>();

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
