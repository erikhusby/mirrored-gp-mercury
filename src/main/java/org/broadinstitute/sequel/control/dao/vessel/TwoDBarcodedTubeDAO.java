package org.broadinstitute.sequel.control.dao.vessel;

import org.broadinstitute.sequel.control.dao.ThreadEntityManager;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.Query;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Data Access Object for tubes.
 */
@Stateful
@RequestScoped
public class TwoDBarcodedTubeDAO {
    @Inject
    private ThreadEntityManager threadEntityManager;

    public Map<String, TwoDBarcodedTube> findByBarcodes(List<String> barcodes) {
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new TreeMap<String, TwoDBarcodedTube>();
        for (String barcode : barcodes) {
            mapBarcodeToTube.put(barcode, null);
        }
        Query query = this.threadEntityManager.getEntityManager().createNamedQuery("TwoDBarcodedTube.fetchByBarcodes");
        @SuppressWarnings("unchecked")
        List<TwoDBarcodedTube> results = (List<TwoDBarcodedTube>) query.setParameter("barcodes", barcodes).getResultList();
        for (TwoDBarcodedTube result : results) {
            mapBarcodeToTube.put(result.getLabel(), result);
        }
        return mapBarcodeToTube;
    }

    // todo jmt need a superclass or delegate for these common methods
    public void persist(TwoDBarcodedTube twoDBarcodedTube) {
        this.threadEntityManager.getEntityManager().persist(twoDBarcodedTube);
    }

    public void flush() {
        this.threadEntityManager.getEntityManager().flush();
    }

    public void clear() {
        this.threadEntityManager.getEntityManager().clear();
    }
}
