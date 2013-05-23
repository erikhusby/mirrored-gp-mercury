package org.broadinstitute.gpinformatics.mercury.control.dao.analysis;

import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessKeyFinder;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.Aligner;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.Aligner_;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence_;

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
public class ReferenceSequenceDao extends GenericDao implements BusinessKeyFinder<ReferenceSequence> {

    public List<ReferenceSequence> findAll() {
        return findAll(ReferenceSequence.class);
    }

    /**
     * Get all the current reference sequences.
     *
     * @return List of all the current {@link ReferenceSequence}s.
     */
    public List<ReferenceSequence> findAllCurrent() {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder();

        final CriteriaQuery<ReferenceSequence> query = criteriaBuilder.createQuery(ReferenceSequence.class);
        Root<ReferenceSequence> root = query.from(ReferenceSequence.class);
        Predicate currentPredicate = criteriaBuilder.equal(root.get(ReferenceSequence_.isCurrent), true);
        query.where(currentPredicate);
        return getEntityManager().createQuery(query).getResultList();
    }

    /**
     * Get all of the reference sequences that match the name.
     *
     * @return List of all the {@link ReferenceSequence}s matching the passed name.
     */
    public List<ReferenceSequence> findAllByName(String name) {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder();
        final CriteriaQuery<ReferenceSequence> query = criteriaBuilder.createQuery(ReferenceSequence.class);
        Root<ReferenceSequence> root = query.from(ReferenceSequence.class);
        Predicate namePredicate = criteriaBuilder.equal(root.get(ReferenceSequence_.name), name);

        query.where(criteriaBuilder.and(namePredicate));
        return getEntityManager().createQuery(query).getResultList();
    }

    @Override
    public ReferenceSequence findByBusinessKey(String businessKey) {
        String[] values = businessKey.split("\\|");
        if (values.length != 2) {
            throw new IllegalArgumentException("Reference Sequence business key must only contain a name and a version: ");
        }

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder();
        final CriteriaQuery<ReferenceSequence> query = criteriaBuilder.createQuery(ReferenceSequence.class);
        Root<ReferenceSequence> root = query.from(ReferenceSequence.class);
        Predicate namePredicate = criteriaBuilder.equal(root.get(ReferenceSequence_.name), values[0]);
        Predicate versionPredicate = criteriaBuilder.equal(root.get(ReferenceSequence_.version), values[1]);

        query.where(criteriaBuilder.and(namePredicate, versionPredicate));

        try {
            return getEntityManager().createQuery(query).getSingleResult();
        } catch (NoResultException exception) {
            // return null if there is no entity
            return null;
        }
    }
}
