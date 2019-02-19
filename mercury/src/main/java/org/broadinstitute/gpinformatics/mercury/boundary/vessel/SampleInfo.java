package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * JAX-RS DTO for the samples in a Sample Kit
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SampleInfo {
    private String manufacturerBarcode;
    private String sampleId;
    private String vesselType;
    private String position;
    private String status;
    private String originalMaterialType;
    private boolean canBeRackScanned;


    /** No-arg constructor required for use by the JAX-RS framework. */
    @SuppressWarnings("UnusedDeclaration")
    public SampleInfo() {
    }

    public SampleInfo(String manufacturerBarcode, String sampleId, String vesselType, String position, String status,
                      String originalMaterialType, boolean canBeRackScanned) {
        this.manufacturerBarcode = manufacturerBarcode;
        this.sampleId = sampleId;
        this.vesselType = vesselType;
        this.position = position;
        this.status = status;
        this.originalMaterialType = originalMaterialType;
        this.canBeRackScanned = canBeRackScanned;
    }

    public String getManufacturerBarcode() {
        return manufacturerBarcode;
    }

    public String getSampleId() {
        return sampleId;
    }

    public String getVesselType() {
        return vesselType;
    }

    public String getPosition() {
        return position;
    }

    public String getStatus() {
        return status;
    }

    public String getOriginalMaterialType() {
        return originalMaterialType;
    }

    public boolean isCanBeRackScanned() {
        return canBeRackScanned;
    }
}
