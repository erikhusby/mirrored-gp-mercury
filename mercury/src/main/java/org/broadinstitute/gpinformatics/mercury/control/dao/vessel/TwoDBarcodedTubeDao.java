package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Data Access Object for tubes.
 */
@Stateful
@RequestScoped
public class TwoDBarcodedTubeDAO extends GenericDao {

    /**
     * Finds tube entities for given barcodes
     * @param barcodes tube barcodes
     * @return map from barcode to tube, tube is null if not found
     */
    public Map<String, TwoDBarcodedTube> findByBarcodes(List<String> barcodes) {
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new TreeMap<>();
        for (String barcode : barcodes) {
            mapBarcodeToTube.put(barcode, null);
        }
        List<TwoDBarcodedTube> results = findListByList(TwoDBarcodedTube.class, TwoDBarcodedTube_.label, barcodes);
        for (TwoDBarcodedTube result : results) {
            mapBarcodeToTube.put(result.getLabel(), result);
        }
        return mapBarcodeToTube;
    }

    public TwoDBarcodedTube findByBarcode(String barcode) {
        return findSingle(TwoDBarcodedTube.class, TwoDBarcodedTube_.label, barcode);
    }
}
