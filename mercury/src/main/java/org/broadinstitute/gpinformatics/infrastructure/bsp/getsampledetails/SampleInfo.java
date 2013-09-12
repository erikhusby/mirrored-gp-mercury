package org.broadinstitute.gpinformatics.infrastructure.bsp.getsampledetails;

import java.io.Serializable;

public class SampleInfo implements Cloneable, Serializable {

    private String sampleId;

    private String wellPosition;

    private String manufacturerBarcode;

    private Float volume;

    private Float concentration;

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getWellPosition() {
        return wellPosition;
    }

    public void setWellPosition(String wellPosition) {
        this.wellPosition = wellPosition;
    }

    public String getManufacturerBarcode() {
        return manufacturerBarcode;
    }

    public void setManufacturerBarcode(String manufacturerBarcode) {
        this.manufacturerBarcode = manufacturerBarcode;
    }

    public Float getVolume() {
        return volume;
    }

    public void setVolume(Float volume) {
        this.volume = volume;
    }

    public Float getConcentration() {
        return concentration;
    }

    public void setConcentration(Float concentration) {
        this.concentration = concentration;
    }
}
