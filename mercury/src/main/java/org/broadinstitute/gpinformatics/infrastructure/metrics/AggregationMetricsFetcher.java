package org.broadinstitute.gpinformatics.infrastructure.metrics;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access object for fetching Picard aggregation metrics.
 *
 * Not a {@link GenericDao} because it uses a different persistence unit.
 */
public class AggregationMetricsFetcher {

    @PersistenceContext(unitName = "metrics_pu")
    private EntityManager entityManager;

    public Aggregation fetch(String project, String sample, int version) {
        return fetch(project, sample, version, null);
    }

    public Aggregation fetch(String project, String sample, int version, String dataType) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Aggregation> criteriaQuery = criteriaBuilder.createQuery(Aggregation.class);
        Root<Aggregation> root = criteriaQuery.from(Aggregation.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.equal(root.get(Aggregation_.project), project));
        predicates.add(criteriaBuilder.equal(root.get(Aggregation_.sample), sample));
        predicates.add(criteriaBuilder.equal(root.get(Aggregation_.version), version));

        // Only query on dataType if it's supplied.
        if (dataType != null) {
            predicates.add(criteriaBuilder.equal(root.get(Aggregation_.dataType), dataType));
        }

        /*
         * Look for the row where LIBRARY is NULL because otherwise there would be multiple results. Kathleen Tibbetts
         * said that this is the way to narrow it down to one.
         */
        predicates.add(criteriaBuilder.isNull(root.get("library")));

        criteriaQuery.where(predicates.toArray(new Predicate[predicates.size()]));

        TypedQuery<Aggregation> query = entityManager.createQuery(criteriaQuery);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
