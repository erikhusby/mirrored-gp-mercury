package org.broadinstitute.gpinformatics.infrastructure.metrics;

import edu.mit.broad.core.util.CollectionUtility;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTuple;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.AggregationReadGroup;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.AggregationReadGroup_;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation_;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.LevelOfDetection;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.PicardAnalysis;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.PicardAnalysis_;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.PicardFingerprint;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.PicardFingerprint_;

import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data access object for fetching Picard aggregation metrics.
 * <p/>
 * Not a {@link GenericDao} because it uses a different persistence unit.
 */
@Stateful
public class AggregationMetricsFetcher {

    private static final Log log = LogFactory.getLog(AggregationMetricsFetcher.class);

    public static final String SAMPLE_COLUMN = "sample";
    public static final String PROJECT_COLUMN = "project";
    public static final String VERSION_COLUMN = "version";
    private static final int MAX_AGGREGATION_FETCHER_QUERY_SIZE = 500;

    @PersistenceContext(unitName = "metrics_pu", type = PersistenceContextType.EXTENDED)
    private EntityManager entityManager;

    public List<Aggregation> fetch(List<SubmissionTuple> tuples) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Aggregation> criteriaQuery = criteriaBuilder.createQuery(Aggregation.class);

        Root<Aggregation> root = criteriaQuery.from(Aggregation.class);
        Collection<Collection<SubmissionTuple>> splitTuples =
                CollectionUtility.split(tuples, MAX_AGGREGATION_FETCHER_QUERY_SIZE);

        List<Aggregation> aggregations = new ArrayList<>();
        for (Collection<SubmissionTuple> subTuples : splitTuples) {
            List<Predicate> predicates = new ArrayList<>();
            for (SubmissionTuple tuple : subTuples) {
                List<Predicate> tuplePredicates = new ArrayList<>();
                tuplePredicates.add(criteriaBuilder.equal(root.get(Aggregation_.project), tuple.getProject()));
                tuplePredicates.add(criteriaBuilder.equal(root.get(Aggregation_.sample), tuple.getSampleName()));
                tuplePredicates.add(criteriaBuilder.equal(root.get(Aggregation_.version), tuple.getVersion()));
                predicates.add(criteriaBuilder.and(tuplePredicates.toArray(new Predicate[tuplePredicates.size()])));
            }

            criteriaQuery.where(criteriaBuilder.or(predicates.toArray(new Predicate[predicates.size()])),
                    criteriaBuilder.isNull(root.get(Aggregation_.library)));

            List<Aggregation> aggregationResult = new ArrayList<>();
            TypedQuery<Aggregation> query = entityManager.createQuery(criteriaQuery);

            try {
                aggregationResult = query.getResultList();
            } catch (NoResultException e) {
                log.info("Unable to retrieve aggregations based on given criteria");
            }
            fetchLod(aggregationResult);
            aggregations.addAll(aggregationResult);
        }
        return aggregations;
    }

    @SuppressWarnings("unchecked")
    public void fetchLod(List<Aggregation> aggregations) {
        if (aggregations.isEmpty()) {
            return;
        }
        CriteriaBuilder queryBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Tuple> tupleQuery = queryBuilder.createTupleQuery();
        Root<PicardFingerprint> root = tupleQuery.from(PicardFingerprint.class);
        Join<PicardFingerprint, PicardAnalysis> fingerPrintAnalysisJoin = root.join(PicardFingerprint_.picardAnalysis);
        Join<PicardAnalysis, AggregationReadGroup> readGroupPicardAnalysisJoin =
                fingerPrintAnalysisJoin.join(PicardAnalysis_.aggregationReadGroups);
        Join<AggregationReadGroup, Aggregation> aggregationAggregationReadGroupsJoin =
                readGroupPicardAnalysisJoin.join(AggregationReadGroup_.aggregation);

        List<Predicate> predicates = new ArrayList<>();
        for (Aggregation aggregation : aggregations) {
            predicates.add(queryBuilder
                    .equal(aggregationAggregationReadGroupsJoin.get(Aggregation_.id), aggregation.getId()));
        }

        Path<Integer> aggregationPath = aggregationAggregationReadGroupsJoin.get(Aggregation_.id);
        Expression<Double> minExpression = queryBuilder.min(root.get(PicardFingerprint_.lodExpectedSample));
        Expression<Double> maxExpression = queryBuilder.max(root.get(PicardFingerprint_.lodExpectedSample));

        CriteriaQuery<Tuple> multiselect = tupleQuery.multiselect(
                aggregationPath, minExpression, maxExpression)
                .where(queryBuilder.or(predicates.toArray(new Predicate[predicates.size()])))
                .groupBy(aggregationPath);

        List<Tuple> tuples = entityManager.createQuery(multiselect).getResultList();
        Map<Integer, LevelOfDetection> lodMap = new HashMap<>();
        for (Tuple tuple : tuples) {
            Integer aggregationId = tuple.get(aggregationPath);
            Double min = tuple.get(minExpression);
            Double max = tuple.get(maxExpression);

            lodMap.put(aggregationId, new LevelOfDetection(min, max));
        }
        for (Aggregation aggregation : aggregations) {
            LevelOfDetection lod = lodMap.get(aggregation.getId());
            if (lod != null) {
                aggregation.setLevelOfDetection(lod);
            }
        }
    }
}
