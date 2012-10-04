package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.broadinstitute.gpinformatics.mercury.control.dao.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

/**
 * Data Access Object for racks of tubes
 */
@Stateful
@RequestScoped
public class RackOfTubesDao extends GenericDao {

    public List<RackOfTubes> findByDigest(String digest) {
        EntityManager entityManager = getThreadEntityManager().getEntityManager();
        CriteriaQuery<RackOfTubes> criteriaQuery =
                entityManager.getCriteriaBuilder().createQuery(RackOfTubes.class);
        Root<RackOfTubes> root = criteriaQuery.from(RackOfTubes.class);
        criteriaQuery.where(entityManager.getCriteriaBuilder().equal(root.get(RackOfTubes_.digest), digest));
        return entityManager.createQuery(criteriaQuery).getResultList();
    }

    public RackOfTubes getByLabel(String rackLabel) {
        EntityManager entityManager = getThreadEntityManager().getEntityManager();
        CriteriaQuery<RackOfTubes> criteriaQuery =
                entityManager.getCriteriaBuilder().createQuery(RackOfTubes.class);
        Root<RackOfTubes> root = criteriaQuery.from(RackOfTubes.class);
        criteriaQuery.where(entityManager.getCriteriaBuilder().equal(root.get(RackOfTubes_.label), rackLabel));
        return entityManager.createQuery(criteriaQuery).getSingleResult();
    }
}
