package org.broadinstitute.gpinformatics.mercury.control.labevent;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

/**
 * Returned by BSP PlateTransferResource.transfer
 */
@XmlRootElement
public class TransferReturn {
    private Map<String, String> mapBarcodeToSmId;

    public TransferReturn() {
    }

    public TransferReturn(Map<String, String> mapBarcodeToSmId) {
        this.mapBarcodeToSmId = mapBarcodeToSmId;
    }

    public Map<String, String> getMapBarcodeToSmId() {
        return mapBarcodeToSmId;
    }

    public void setMapBarcodeToSmId(Map<String, String> mapBarcodeToSmId) {
        this.mapBarcodeToSmId = mapBarcodeToSmId;
    }
}
