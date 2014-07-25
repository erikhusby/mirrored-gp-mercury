package org.broadinstitute.gpinformatics.infrastructure.metrics;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation_;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.LevelOfDetection;

import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access object for fetching Picard aggregation metrics.
 * <p/>
 * Not a {@link GenericDao} because it uses a different persistence unit.
 */
@Stateful
public class AggregationMetricsFetcher {
    public static final String SAMPLE_COLUMN = "sample";
    public static final String PROJECT_COLUMN = "project";
    public static final String VERSION_COLUMN = "version";

    @PersistenceContext(unitName = "metrics_pu", type = PersistenceContextType.EXTENDED)
    private EntityManager entityManager;

    public Aggregation fetch(String project, String sample, int version) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Aggregation> criteriaQuery = criteriaBuilder.createQuery(Aggregation.class);
        Root<Aggregation> root = criteriaQuery.from(Aggregation.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.equal(root.get(Aggregation_.project), project));
        if (sample != null) {
            predicates.add(criteriaBuilder.equal(root.get(Aggregation_.sample), sample));
        }
        predicates.add(criteriaBuilder.equal(root.get(Aggregation_.aggregationVersion), version));

        /*
         * Look for the row where LIBRARY is NULL because otherwise there would be multiple results. Kathleen Tibbetts
         * said that this is the way to narrow it down to one.
         */
        predicates.add(criteriaBuilder.isNull(root.get("library")));

        criteriaQuery.where(predicates.toArray(new Predicate[predicates.size()]));

        TypedQuery<Aggregation> query = entityManager.createQuery(criteriaQuery);
        Aggregation aggregation;
        try {
            aggregation = query.getSingleResult();
            TypedQuery<LevelOfDetection> lodQuery =
                    entityManager.createNamedQuery(LevelOfDetection.LOD_QUERY_NAME, LevelOfDetection.class)
                            .setParameter(SAMPLE_COLUMN, aggregation.getSample())
                            .setParameter(PROJECT_COLUMN, aggregation.getProject())
                            .setParameter(VERSION_COLUMN, aggregation.getAggregationVersion());
            aggregation.setLevelOfDetection(lodQuery.getSingleResult());
        } catch (NoResultException e) {
            aggregation = null;
        }
        return aggregation;
    }

}
