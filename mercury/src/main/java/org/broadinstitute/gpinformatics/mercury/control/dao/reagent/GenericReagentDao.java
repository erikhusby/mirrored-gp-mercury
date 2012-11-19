package org.broadinstitute.gpinformatics.mercury.control.dao.reagent;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.GenericReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.GenericReagent_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/**
 * Data Access object for generic reagents
 */
@Stateful
@RequestScoped
public class GenericReagentDao extends GenericDao {

    public GenericReagent findByReagentNameAndLot(String reagentName, String lot) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<GenericReagent> criteriaQuery = criteriaBuilder.createQuery(GenericReagent.class);
        Root<GenericReagent> root = criteriaQuery.from(GenericReagent.class);
        criteriaQuery.where(criteriaBuilder.equal(root.get(GenericReagent_.reagentName), reagentName));
        criteriaQuery.where(criteriaBuilder.equal(root.get(GenericReagent_.lot), lot));
        try {
            return getEntityManager().createQuery(criteriaQuery).getSingleResult();
        } catch (NoResultException ignored) {
            return null;
        }
    }
}
