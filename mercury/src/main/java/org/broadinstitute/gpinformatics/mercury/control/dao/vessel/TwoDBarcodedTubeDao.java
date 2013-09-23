package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube_;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Data Access Object for tubes.
 */
@Stateful
@RequestScoped
public class TwoDBarcodedTubeDao extends GenericDao {

    /**
     * Returns a simple List of {@code TwoDBarcodedTube}s for the input {@code barcodes}.
     */
    public List<TwoDBarcodedTube> findListByBarcodes(@Nonnull Collection<String> barcodes) {
        return findListByList(TwoDBarcodedTube.class, TwoDBarcodedTube_.label, barcodes);
    }

    /**
     * Finds tube entities for given barcodes
     *
     * @param barcodes tube barcodes
     *
     * @return map from barcode to tube, tube is null if not found
     */
    public Map<String, TwoDBarcodedTube> findByBarcodes(@Nonnull Collection<String> barcodes) {
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

    public TwoDBarcodedTube findByBarcode(@Nonnull String barcode) {
        return findSingle(TwoDBarcodedTube.class, TwoDBarcodedTube_.label, barcode);
    }
}
