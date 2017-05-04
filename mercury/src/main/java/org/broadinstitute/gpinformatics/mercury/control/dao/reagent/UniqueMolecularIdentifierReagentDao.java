package org.broadinstitute.gpinformatics.mercury.control.dao.reagent;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.GenericReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent_;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UniqueMolecularIdentifierReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UniqueMolecularIdentifierReagent_;

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

    public UniqueMolecularIdentifierReagent findByLocationAndLength(
            UniqueMolecularIdentifierReagent.UMILocation location, int length) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<UniqueMolecularIdentifierReagent> criteriaQuery =
                criteriaBuilder.createQuery(UniqueMolecularIdentifierReagent.class);
        Root<UniqueMolecularIdentifierReagent> root = criteriaQuery.from(UniqueMolecularIdentifierReagent.class);
        criteriaQuery.where(criteriaBuilder.and(
                criteriaBuilder.equal(root.get(UniqueMolecularIdentifierReagent_.umiLocation), location),
                criteriaBuilder.equal(root.get(UniqueMolecularIdentifierReagent_.length), length)));
        try {
            return getEntityManager().createQuery(criteriaQuery).getSingleResult();
        } catch (NoResultException ignored) {
            return null;
        }
    }
}
