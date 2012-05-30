package org.broadinstitute.sequel.control.dao.vessel;

import org.broadinstitute.sequel.control.dao.GenericDao;
import org.broadinstitute.sequel.entity.vessel.StripTube;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.Query;

/**
 * Data Access Object for Strip tubes
 */
@Stateful
@RequestScoped
public class StripTubeDao extends GenericDao {
    public StripTube findByBarcode(String barcode) {
        StripTube stripTube = null;
        Query query = this.getThreadEntityManager().getEntityManager().createNamedQuery("StripTube.findByBarcode");
        try {
            stripTube = (StripTube) query.setParameter("barcode", barcode).getSingleResult();
        } catch (NoResultException ignored) {
        }
        return stripTube;
    }
}
