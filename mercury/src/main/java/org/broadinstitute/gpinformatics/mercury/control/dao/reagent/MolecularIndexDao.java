package org.broadinstitute.gpinformatics.mercury.control.dao.reagent;

import org.broadinstitute.gpinformatics.mercury.control.dao.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex_;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/**
 * Data Access Object for Molecular Index
 */
public class MolecularIndexDao extends GenericDao {
    public MolecularIndex findBySequence(String sequence) {
        EntityManager entityManager = getThreadEntityManager().getEntityManager();
        CriteriaQuery<MolecularIndex> criteriaQuery =
                entityManager.getCriteriaBuilder().createQuery(MolecularIndex.class);
        Root<MolecularIndex> root = criteriaQuery.from(MolecularIndex.class);
        criteriaQuery.where(entityManager.getCriteriaBuilder().equal(root.get(MolecularIndex_.sequence), sequence));

        MolecularIndex molecularIndex = null;
        try {
            molecularIndex = entityManager.createQuery(criteriaQuery).getSingleResult();
        } catch (NoResultException ignored) {
        }
        return molecularIndex;
    }
}
