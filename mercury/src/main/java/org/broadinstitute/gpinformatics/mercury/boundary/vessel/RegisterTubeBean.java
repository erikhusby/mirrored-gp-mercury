package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class RegisterTubeBean {

    private String barcode;
    private String well;
    private String sampleId;

    public RegisterTubeBean() {
    }

    public RegisterTubeBean(@Nonnull String barcode, @Nullable String well, @Nullable String sampleId) {
        this.barcode = barcode;
        this.well = well;
        this.sampleId = sampleId;
    }

    public String getBarcode() {
        return barcode;
    }

    public String getWell() {
        return well;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public void setWell(String well) {
        this.well = well;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }
}
