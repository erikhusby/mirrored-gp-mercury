package org.broadinstitute.gpinformatics.mercury.control.dao.reagent;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UniqueMolecularIdentifier;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.UniqueMolecularIdentifier_;

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

    public UniqueMolecularIdentifier findByLocationAndLength(
            UniqueMolecularIdentifier.UMILocation location, long length, long spacerLength) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<UniqueMolecularIdentifier> criteriaQuery =
                criteriaBuilder.createQuery(UniqueMolecularIdentifier.class);
        Root<UniqueMolecularIdentifier> root = criteriaQuery.from(UniqueMolecularIdentifier.class);
        criteriaQuery.where(
                getCriteriaBuilder().equal(root.get(UniqueMolecularIdentifier_.location), location),
                getCriteriaBuilder().equal(root.get(UniqueMolecularIdentifier_.spacerLength), spacerLength),
                getCriteriaBuilder().equal(root.get(UniqueMolecularIdentifier_.length), length));
        try {
            return getEntityManager().createQuery(criteriaQuery).getSingleResult();
        } catch (NoResultException ignored) {
            return null;
        }
    }
}
