package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DDPBarcodeRegistration {
    private String barcode;
    private Boolean dsmKit;

    public DDPBarcodeRegistration() {
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public Boolean getDsmKit() {
        return dsmKit;
    }

    public void setDsmKit(Boolean dsmKit) {
        this.dsmKit = dsmKit;
    }
}
