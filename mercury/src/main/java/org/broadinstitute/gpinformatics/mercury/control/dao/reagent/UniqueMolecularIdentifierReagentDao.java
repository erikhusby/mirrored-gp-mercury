package org.broadinstitute.gpinformatics.mercury.control.dao.reagent;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UMIReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UMIReagent_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/**
 * Data Access Object for UMI Reagents.
 */
@Stateful
@RequestScoped
public class UniqueMolecularIdentifierReagentDao extends GenericDao {

    public UMIReagent findByLocationAndLength(
            UMIReagent.UMILocation location, long length) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<UMIReagent> criteriaQuery =
                criteriaBuilder.createQuery(UMIReagent.class);
        Root<UMIReagent> root = criteriaQuery.from(UMIReagent.class);
        criteriaQuery.where(
                getCriteriaBuilder().equal(root.get(UMIReagent_.umiLocation), location),
                getCriteriaBuilder().equal(root.get(UMIReagent_.umiLength), length));
        try {
            return getEntityManager().createQuery(criteriaQuery).getSingleResult();
        } catch (NoResultException ignored) {
            return null;
        }
    }
}
