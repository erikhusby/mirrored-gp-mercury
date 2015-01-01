package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube_;

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
public class BarcodedTubeDao extends GenericDao {

    /**
     * Returns a simple List of {@code BarcodedTube}s for the input {@code barcodes}.
     */
    public List<BarcodedTube> findListByBarcodes(@Nonnull Collection<String> barcodes) {
        return findListByList(BarcodedTube.class, BarcodedTube_.label, barcodes);
    }

    /**
     * Finds tube entities for given barcodes
     *
     * @param barcodes tube barcodes
     *
     * @return map from barcode to tube, tube is null if not found
     */
    public Map<String, BarcodedTube> findByBarcodes(@Nonnull Collection<String> barcodes) {
        Map<String, BarcodedTube> mapBarcodeToTube = new TreeMap<>();
        for (String barcode : barcodes) {
            mapBarcodeToTube.put(barcode, null);
        }
        List<BarcodedTube> results = findListByList(BarcodedTube.class, BarcodedTube_.label, barcodes);
        for (BarcodedTube result : results) {
            mapBarcodeToTube.put(result.getLabel(), result);
        }
        return mapBarcodeToTube;
    }

    public BarcodedTube findByBarcode(@Nonnull String barcode) {
        return findSingle(BarcodedTube.class, BarcodedTube_.label, barcode);
    }
}
