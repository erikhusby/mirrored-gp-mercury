package org.broadinstitute.gpinformatics.mercury.presentation.labevent;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to map & persist JSON data back to the transfer_plate_strip_tube JSP page.
 *
 */
public class StripTubePositions
{
    public List<String> fct = new ArrayList<String>();
    public List<String> stripTubeBarcode = new ArrayList<String>();
    public String barcodeValue;
    public String fctValue;
    public String connectionPositions;

    /**
     * Gets the value of the fct property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getFctValue() {
        return fctValue;
    }
    /**
     * Gets the value of the barcode property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getBarcodeValue() {
        return barcodeValue;
    }
}
