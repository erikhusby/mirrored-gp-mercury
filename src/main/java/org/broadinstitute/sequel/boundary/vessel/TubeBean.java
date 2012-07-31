package org.broadinstitute.sequel.boundary.vessel;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Used to import batches of tubes from Squid and BSP
 */
@XmlRootElement
public class TubeBean {
    private String barcode;
    private String sampleBarcode;

    public TubeBean(String barcode, String sampleBarcode) {
        this.barcode = barcode;
        this.sampleBarcode = sampleBarcode;
    }

    public TubeBean() {
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getSampleBarcode() {
        return sampleBarcode;
    }

    public void setSampleBarcode(String sampleBarcode) {
        this.sampleBarcode = sampleBarcode;
    }
}
