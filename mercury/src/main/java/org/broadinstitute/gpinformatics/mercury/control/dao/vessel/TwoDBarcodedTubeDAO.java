package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.broadinstitute.gpinformatics.mercury.control.dao.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Data Access Object for tubes.
 */
@Stateful
@RequestScoped
public class TwoDBarcodedTubeDAO extends GenericDao {

    public Map<String, TwoDBarcodedTube> findByBarcodes(List<String> barcodes) {
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new TreeMap<String, TwoDBarcodedTube>();
        for (String barcode : barcodes) {
            mapBarcodeToTube.put(barcode, null);
        }
        Query query = this.getThreadEntityManager().getEntityManager().createNamedQuery("TwoDBarcodedTube.fetchByBarcodes");
        @SuppressWarnings("unchecked")
        List<TwoDBarcodedTube> results = (List<TwoDBarcodedTube>) query.setParameter("barcodes", barcodes).getResultList();
        for (TwoDBarcodedTube result : results) {
            mapBarcodeToTube.put(result.getLabel(), result);
        }
        return mapBarcodeToTube;
    }

    public TwoDBarcodedTube findByBarcode(String barcode) {
        TwoDBarcodedTube twoDBarcodedTube = null;
        try {
            twoDBarcodedTube = (TwoDBarcodedTube) this.getThreadEntityManager().getEntityManager().
                    createNamedQuery("TwoDBarcodedTube.fetchByBarcode").
                    setParameter("barcode", barcode).getSingleResult();
        } catch (NoResultException ignored) {
        }
        return twoDBarcodedTube;
    }
}
