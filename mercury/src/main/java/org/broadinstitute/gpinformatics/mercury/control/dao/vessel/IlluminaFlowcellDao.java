package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.broadinstitute.gpinformatics.mercury.control.dao.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/**
 * Data Access Object for flowcells
 */
@Stateful
@RequestScoped
public class IlluminaFlowcellDao extends GenericDao {
    public IlluminaFlowcell findByBarcode(String barcode) {
        IlluminaFlowcell illuminaFlowcell = null;
        EntityManager entityManager = getThreadEntityManager().getEntityManager();
        CriteriaQuery<IlluminaFlowcell> criteriaQuery =
                entityManager.getCriteriaBuilder().createQuery(IlluminaFlowcell.class);
        Root<IlluminaFlowcell> root = criteriaQuery.from(IlluminaFlowcell.class);
        criteriaQuery.where(entityManager.getCriteriaBuilder().equal(root.get(IlluminaFlowcell_.label), barcode));
        try {
            illuminaFlowcell = entityManager.createQuery(criteriaQuery).getSingleResult();
        } catch (NoResultException ignored) {
        }
        return illuminaFlowcell;
    }
}
