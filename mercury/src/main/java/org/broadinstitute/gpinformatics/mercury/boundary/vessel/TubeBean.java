package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.mercury.boundary.Namespaces;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Used to import batches of tubes from Squid and BSP
 * todo jmt need to support DBA cards too
 */
@XmlRootElement(namespace = Namespaces.VESSEL)
@XmlType(namespace = Namespaces.VESSEL)
public class TubeBean {
    private String barcode;
    private String sampleBarcode;
    private String productOrderKey;

    public TubeBean(String barcode, String sampleBarcode, String productOrderKey) {
        this.barcode = barcode;
        this.sampleBarcode = sampleBarcode;
        this.productOrderKey = productOrderKey;
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

    public String getProductOrderKey() {
        return productOrderKey;
    }

    public void setProductOrderKey(String productOrderKey) {
        this.productOrderKey = productOrderKey;
    }
}
