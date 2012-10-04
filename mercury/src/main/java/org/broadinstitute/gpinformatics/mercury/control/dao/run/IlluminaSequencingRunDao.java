package org.broadinstitute.gpinformatics.mercury.control.dao.run;

import org.broadinstitute.gpinformatics.mercury.control.dao.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/**
 * Data Access Object for sequencing runs
 */
@Stateful
@RequestScoped
public class IlluminaSequencingRunDao extends GenericDao{

    public IlluminaSequencingRun findByRunName(String runName) {
        EntityManager entityManager = getThreadEntityManager().getEntityManager();
        CriteriaQuery<IlluminaSequencingRun> criteriaQuery =
                entityManager.getCriteriaBuilder().createQuery(IlluminaSequencingRun.class);
        Root<IlluminaSequencingRun> root = criteriaQuery.from(IlluminaSequencingRun.class);
        criteriaQuery.where(entityManager.getCriteriaBuilder().equal(root.get(IlluminaSequencingRun_.runName), runName));
        IlluminaSequencingRun illuminaSequencingRun = null;
        try {
            illuminaSequencingRun = entityManager.createQuery(criteriaQuery).getSingleResult();
        } catch (NoResultException ignored) {
        }
        return illuminaSequencingRun;
    }
}
