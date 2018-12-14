package org.broadinstitute.gpinformatics.infrastructure.metrics;

import com.google.common.collect.Iterables;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTuple;
import org.broadinstitute.gpinformatics.infrastructure.cognos.entity.PicardAggregationSample;
import org.broadinstitute.gpinformatics.infrastructure.cognos.entity.PicardAggregationSample_;
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
import javax.enterprise.context.RequestScoped;
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
@RequestScoped
public class AggregationMetricsFetcher {

    private static final Log log = LogFactory.getLog(AggregationMetricsFetcher.class);

    private static final int MAX_AGGREGATION_FETCHER_QUERY_SIZE = 1000;

    @PersistenceContext(unitName = "metrics_pu", type = PersistenceContextType.EXTENDED)
    private EntityManager entityManager;

    public List<Aggregation> fetch(Collection<SubmissionTuple> tuples) {

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Aggregation> criteriaQuery = criteriaBuilder.createQuery(Aggregation.class);
        Root<Aggregation> root = criteriaQuery.from(Aggregation.class);
        root.fetch(Aggregation_.aggregationWgs);
        Join<Aggregation, PicardAggregationSample> picardAggregationSampleJoin = root.join(Aggregation_.picardAggregationSample);

        List<Aggregation> allResults = new ArrayList<>();
        Map<String, Collection<SubmissionTuple>> tuplesByProject = SubmissionTuple.byProject(tuples);

        for (Map.Entry<String, Collection<SubmissionTuple>> projectTupleEntry : tuplesByProject.entrySet()) {
            String projectName = projectTupleEntry.getKey();
            Collection<SubmissionTuple> tupleList = projectTupleEntry.getValue();
            for (List<SubmissionTuple> tuplesSublist : Iterables.partition(tupleList, MAX_AGGREGATION_FETCHER_QUERY_SIZE)) {
                List<Aggregation> aggregations = new ArrayList<>();
                List<Predicate> predicates = new ArrayList<>();
                Predicate projectJoin = criteriaBuilder.and(
                    criteriaBuilder.or(
                        criteriaBuilder.equal(
                            picardAggregationSampleJoin.get(PicardAggregationSample_.researchProject), projectName),
                        criteriaBuilder.equal(
                            picardAggregationSampleJoin.get(PicardAggregationSample_.project), projectName)
                ));
                predicates.add(projectJoin);
                predicates.add(criteriaBuilder.isNull(root.get(Aggregation_.library)));
                predicates.add(criteriaBuilder.isTrue(root.get(Aggregation_.latest)));
                predicates.add(root.get(Aggregation_.sample).in(SubmissionTuple.extractSampleNames(tuplesSublist)));

                CriteriaQuery<Aggregation> whereClause =
                    criteriaQuery.where(predicates.toArray(new Predicate[predicates.size()]));
                TypedQuery<Aggregation> query = entityManager.createQuery(whereClause.distinct(true));

                try {
                    List<Aggregation> resultList = query.getResultList();
                    aggregations.addAll(resultList);
                } catch (NoResultException e) {
                    log.info("Unable to retrieve aggregations based on given criteria");
                }
                allResults.addAll(aggregations);
            }
        }
        for (List<Aggregation> aggregations : Iterables.partition(allResults, MAX_AGGREGATION_FETCHER_QUERY_SIZE)) {
            fetchLod(aggregations);
        }
        return allResults;
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
        List<Integer> aggregationIds = new ArrayList<>(aggregations.size());
        for (Aggregation aggregation : aggregations) {
            aggregationIds.add(aggregation.getId());
            predicates.add(queryBuilder
                    .equal(aggregationAggregationReadGroupsJoin.get(Aggregation_.id), aggregation.getId()));
        }

        Path<Integer> aggregationPath = aggregationAggregationReadGroupsJoin.get(Aggregation_.id);
        Expression<Double> minExpression = queryBuilder.min(root.get(PicardFingerprint_.lodExpectedSample));
        Expression<Double> maxExpression = queryBuilder.max(root.get(PicardFingerprint_.lodExpectedSample));

        CriteriaQuery<Tuple> multiselect = tupleQuery.multiselect(aggregationPath, minExpression, maxExpression)
            .where(aggregationPath.in(aggregationIds)).groupBy(aggregationPath);

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
