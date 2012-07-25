package org.broadinstitute.sequel.entity.sample;
// todo jmt this should be in the control.dao package

import org.broadinstitute.sequel.control.dao.GenericDao;
import org.broadinstitute.sequel.entity.bsp.BSPStartingSample;

import javax.persistence.NoResultException;

/**
 */
public class BSPStartingSampleDAO extends GenericDao {

    public BSPStartingSample findBySampleName(String stockName) {
        BSPStartingSample bspStartingSample = null;
        try {
            bspStartingSample = (BSPStartingSample) this.getThreadEntityManager().getEntityManager().
                    createNamedQuery("BSPStartingSample.fetchBySampleName").
                    setParameter("sampleName", stockName).getSingleResult();
        } catch (NoResultException ignored) {
        }
        return bspStartingSample;
    }
}
