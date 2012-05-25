package org.broadinstitute.sequel.control.dao.vessel;

import org.broadinstitute.sequel.control.dao.GenericDao;
import org.broadinstitute.sequel.entity.vessel.StaticPlate;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.Query;

/**
 * Data Access Object for plates
 */
@Stateful
@RequestScoped
public class StaticPlateDAO extends GenericDao {

    public StaticPlate findByBarcode(String barcode) {
        Query query = this.getThreadEntityManager().getEntityManager().createNamedQuery("StaticPlate.findByBarcode");
        query.setParameter("barcode", barcode);
        StaticPlate staticPlate = null;
        try {
            staticPlate = (StaticPlate) query.getSingleResult();
        } catch (NoResultException ignored) {
        }
        return staticPlate;
    }
}
