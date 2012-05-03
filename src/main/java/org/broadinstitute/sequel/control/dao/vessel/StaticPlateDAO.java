package org.broadinstitute.sequel.control.dao.vessel;

import org.broadinstitute.sequel.control.dao.ThreadEntityManager;
import org.broadinstitute.sequel.entity.vessel.StaticPlate;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.persistence.Query;

/**
 * Data Access Object for plates
 */
@Stateful
@RequestScoped
public class StaticPlateDAO {
    @Inject
    private ThreadEntityManager threadEntityManager;

    public StaticPlate findByBarcode(String barcode) {
        Query query = this.threadEntityManager.getEntityManager().createNamedQuery("StaticPlate.findByBarcode");
        query.setParameter("barcode", barcode);
        StaticPlate staticPlate = null;
        try {
            staticPlate = (StaticPlate) query.getSingleResult();
        } catch (NoResultException ignored) {
        }
        return staticPlate;
    }
}
