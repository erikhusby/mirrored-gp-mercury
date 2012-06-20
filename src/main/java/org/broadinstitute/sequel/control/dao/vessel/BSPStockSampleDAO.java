package org.broadinstitute.sequel.control.dao.vessel;

import org.broadinstitute.sequel.control.dao.GenericDao;
import org.broadinstitute.sequel.entity.vessel.BSPStockSample;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Data Access Object for BSP stock/aliquot tubes.
 */
@Stateful
@RequestScoped
public class BSPStockSampleDAO extends GenericDao {

    public Map<String, BSPStockSample> findByBarcodes(List<String> barcodes) {
        Map<String, BSPStockSample> mapBarcodeToTube = new TreeMap<String, BSPStockSample>();


        for (String barcode : barcodes) {
            mapBarcodeToTube.put(barcode, null);
        }
        Query query = this.getThreadEntityManager().getEntityManager().createNamedQuery("BSPStockSample.fetchByBarcodes");
        @SuppressWarnings("unchecked")
        List<BSPStockSample> results = (List<BSPStockSample>) query.setParameter("barcodes", barcodes).getResultList();
        for (BSPStockSample result : results) {
            mapBarcodeToTube.put(result.getLabel(), result);
        }
        return mapBarcodeToTube;
    }

    public BSPStockSample findByBarcode(String barcode) {
        BSPStockSample BSPStockSample = null;
        try {
            BSPStockSample = (BSPStockSample) this.getThreadEntityManager().getEntityManager().
                    createNamedQuery("BSPStockSample.fetchByBarcode").
                    setParameter("barcode", barcode).getSingleResult();
        } catch (NoResultException ignored) {
        }
        return BSPStockSample;
    }
}
