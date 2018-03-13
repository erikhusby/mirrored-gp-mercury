package org.broadinstitute.gpinformatics.mercury.control.dao.analysis;

import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessObjectFinder;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence_;

import javax.annotation.Nonnull;
import javax.ejb.LocalBean;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;

/**
 * Data Access for Analysis types.
 */
@Stateful
@LocalBean
@RequestScoped
public class ReferenceSequenceDao extends GenericDao implements BusinessObjectFinder<ReferenceSequence> {

    public List<ReferenceSequence> findAll() {
        return findAll(ReferenceSequence.class);
    }

    /**
     * Get all the current reference sequences.
     *
     * @return list of all the current {@link ReferenceSequence}s
     */
    public List<ReferenceSequence> findAllCurrent() {
        return findList(ReferenceSequence.class, ReferenceSequence_.isCurrent, true);
    }

    /**
     * Get the current reference sequence of a given name.
     *
     * @return The current {@link ReferenceSequence}s if it exists
     */
    public ReferenceSequence findCurrent(@Nonnull String name) {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder();

        final CriteriaQuery<ReferenceSequence> query = criteriaBuilder.createQuery(ReferenceSequence.class);
        Root<ReferenceSequence> root = query.from(ReferenceSequence.class);
        Predicate currentPredicate = criteriaBuilder.equal(root.get(ReferenceSequence_.isCurrent), true);
        Predicate namePredicate = criteriaBuilder.equal(root.get(ReferenceSequence_.name), name);
        query.where(criteriaBuilder.and(currentPredicate, namePredicate));

        try {
            return getEntityManager().createQuery(query).getSingleResult();
        } catch (NoResultException exception) {
            // return null if there is no entity
            return null;
        }
    }

    /**
     * Find the current reference sequence of a given name and version.
     *
     * @param name    the display name of the {@link ReferenceSequence}
     * @param version the version of the {@link ReferenceSequence}
     * @return The current {@link ReferenceSequence}s if it exists or null if it is not found
     */
    public ReferenceSequence findByNameAndVersion(@Nonnull String name, @Nonnull String version) {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder();

        final CriteriaQuery<ReferenceSequence> query = criteriaBuilder.createQuery(ReferenceSequence.class);
        Root<ReferenceSequence> root = query.from(ReferenceSequence.class);
        Predicate namePredicate = criteriaBuilder.equal(root.get(ReferenceSequence_.name), name);
        Predicate versionPredicate = criteriaBuilder.equal(root.get(ReferenceSequence_.version), version);
        query.where(criteriaBuilder.and(namePredicate, versionPredicate));

        try {
            return getEntityManager().createQuery(query).getSingleResult();
        } catch (NoResultException exception) {
            // return null if there is no entity
            return null;
        }
    }

    @Override
    public ReferenceSequence findByBusinessKey(@Nonnull String businessKey) {
        String[] values = businessKey.split("\\" + ReferenceSequence.SEPARATOR);

        if (values.length != 2) {
            throw new IllegalArgumentException("Reference Sequence business key must only contain a name and a version: ");
        }
        return findByNameAndVersion(values[0], values[1]);
    }
}
