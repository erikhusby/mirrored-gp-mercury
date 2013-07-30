package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPPlatingReceipt;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.Query;

/**
 * Data Access Object for BSP stock/aliquot tubes.
 */
@Stateful
@RequestScoped
public class BSPPlatingReceiptDAO extends GenericDao {

    public BSPPlatingReceipt findByReceipt(String receipt) {

        BSPPlatingReceipt bspPlatingReceipt = null;
        Query query = getEntityManager().createNamedQuery("BSPPlatingReceipt.fetchByReceipt");
        try {
            bspPlatingReceipt = (BSPPlatingReceipt) query.setParameter("bspReceipt", receipt).getSingleResult();
        } catch (NoResultException ignored) {
        }
        return bspPlatingReceipt;
    }
}

