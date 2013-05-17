package org.broadinstitute.gpinformatics.mercury.control.dao.analysis;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;

/**
 * Data Access for Analysis types.
 */
@Stateful
@RequestScoped
public class ReferenceSequenceDao extends GenericDao {

    public List<ReferenceSequence> findAll() {
        return super.findAll(ReferenceSequence.class);
    }

    public ReferenceSequence findByBusinessKey(String value) {
        String[] values = value.split("|");

        CriteriaBuilder criteriaBuilder = getCriteriaBuilder();
        final CriteriaQuery<ReferenceSequence> query = criteriaBuilder.createQuery(ReferenceSequence.class);
        Root<ReferenceSequence> root = query.from(ReferenceSequence.class);
        Predicate namePredicate = criteriaBuilder.equal(root.get(ReferenceSequence_.name), values[0]);
        Predicate versionPredicate = criteriaBuilder.equal(root.get(ReferenceSequence_.version), values[1]);

        query.where(criteriaBuilder.and(namePredicate, versionPredicate));
        return getEntityManager().createQuery(query).getSingleResult();
    }
}
