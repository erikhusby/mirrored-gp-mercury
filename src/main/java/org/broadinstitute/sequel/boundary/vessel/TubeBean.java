package org.broadinstitute.sequel.boundary.vessel;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Used to import racks of tubes from Squid
 */
@XmlRootElement
public class TubeBean {
    public String barcode;
    public String position;
    public String sampleBarcode;

    public TubeBean(String barcode, String position, String sampleBarcode) {
        this.barcode = barcode;
        this.position = position;
        this.sampleBarcode = sampleBarcode;
    }

    public TubeBean() {
    }
}
