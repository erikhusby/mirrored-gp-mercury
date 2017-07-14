package org.broadinstitute.gpinformatics.infrastructure.metrics;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
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
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;
import org.hibernate.ejb.criteria.predicate.CompoundPredicate;

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

    private static final int MAX_AGGREGATION_FETCHER_QUERY_SIZE = 500;

    @PersistenceContext(unitName = "metrics_pu", type = PersistenceContextType.EXTENDED)
    private EntityManager entityManager;

    public List<Aggregation> fetch(String researchProject,
                                   MessageReporter messageReporter) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Aggregation> criteriaQuery = criteriaBuilder.createQuery(Aggregation.class);
        Root<Aggregation> root = criteriaQuery.from(Aggregation.class);
        Predicate projectPredicate = criteriaBuilder.equal(root.get(Aggregation_.project), researchProject);
            Predicate latestPredicate = criteriaBuilder.equal(root.get(Aggregation_.latest), true);
        criteriaQuery.where(criteriaBuilder
            .and(projectPredicate, latestPredicate,
                criteriaBuilder.isNull(root.get(Aggregation_.library))
            )
        );


        TypedQuery<Aggregation> query = entityManager.createQuery(criteriaQuery);
        List<Aggregation> aggregations = new ArrayList<>();
        try {
            aggregations.addAll(query.getResultList());
        } catch (NoResultException e) {
            log.info("Unable to retrieve aggregations based on given criteria");
        }

        fetchLod(aggregations);
        return aggregations;
    }

    public List<Aggregation> fetch(String researchProject, Collection<SubmissionTuple> tuples, MessageReporter messageReporter) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Aggregation> criteriaQuery = criteriaBuilder.createQuery(Aggregation.class);
        Root<Aggregation> root = criteriaQuery.from(Aggregation.class);
        HashMultimap<String, SubmissionTuple> tuplesByProject = HashMultimap.create();
        for (SubmissionTuple tuple : tuples) {
            tuplesByProject.put(tuple.getProject(), tuple);
        }

        List<Aggregation> allResults = new ArrayList<>();
        for (List<SubmissionTuple> tuplesSublist : Iterables.partition(tuples, 1000)) {
            List<Aggregation> aggregations = new ArrayList<>();

            SubmissionTuple aTuple = tuplesSublist.iterator().next();
            Predicate versionPredicate = null;
            if (aTuple.getVersion() == null) {
                versionPredicate = criteriaBuilder.equal(root.get(Aggregation_.latest), true);
            } else {
                versionPredicate = criteriaBuilder.equal(root.get(Aggregation_.version), aTuple.getVersion());
            }

            Predicate projectPredicate = criteriaBuilder.equal(root.get(Aggregation_.project), researchProject);
            Predicate latestPredicate = criteriaBuilder.equal(root.get(Aggregation_.latest), true);
            criteriaQuery.where(criteriaBuilder
                .and(projectPredicate, latestPredicate,
                    criteriaBuilder.isNull(root.get(Aggregation_.library)),
                    getTupleExpression(new ArrayList<SubmissionTuple>(tuplesSublist), root)
                )
            );


            TypedQuery<Aggregation> query = entityManager.createQuery(criteriaQuery);

            try {
                aggregations.addAll(query.getResultList());
            } catch (NoResultException e) {
                log.info("Unable to retrieve aggregations based on given criteria");
            }

        List<Aggregation> results = new ArrayList<>();

        for (Aggregation aggregation : aggregations) {
            Map<String, Collection<SubmissionTuple>> tuplesBySample =
                SubmissionTuple.sampleMap(tuples, aggregation.getTuple());
            Collection<SubmissionTuple> foundTuples = tuplesBySample.get(aggregation.getSample());
            if (foundTuples != null) {
                if (foundTuples.size() > 1) {
                    messageReporter
                        .addMessage("Ambiguous tuple found for {1}: {2}", aggregation.getSample(), foundTuples);
                } else if (!foundTuples.isEmpty()) {
                    if (!results.contains(aggregation)) {
                        results.add(aggregation);
                    }
                }
            }
        }

        fetchLod(results);
        allResults.addAll(results);
    }
//        results.addAll(results);
        return allResults;
    }

    private CompoundPredicate getTupleExpression(List<SubmissionTuple> tuples, Root<Aggregation> root) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CompoundPredicate and = (CompoundPredicate) criteriaBuilder.conjunction();

        Expression<String> sampleExpression = root.get(Aggregation_.sample);
        Predicate in = root.get(Aggregation_.sample).in(SubmissionTuple.samples(tuples));

        and.getExpressions().add(in);

        return and;
    }

//    private Collection<SubmissionTuple> collectTuples(List<ProductOrderSample> productOrderSamples) {
//        return Collections2.transform(productOrderSamples,
//                new Function<ProductOrderSample, SubmissionTuple>() {
//                    @Override
//                    public SubmissionTuple apply(@Nullable ProductOrderSample input) {
//                        return new SubmissionTuple(input.getProductOrder().getResearchProject().getJiraTicketKey(),
//                                input.getSampleData().getCollaboratorsSampleName(), null);
//                    }
//                });
//    }

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

        CriteriaQuery<Tuple> multiselect = tupleQuery.multiselect(
                aggregationPath, minExpression, maxExpression)
            .where(aggregationPath.in(aggregationIds))
//                .where(queryBuilder.or(predicates.toArray(new Predicate[predicates.size()])))
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

    public List<Aggregation> fetch(String researchProject, Collection<SubmissionTuple> submissionTuples) {
        return fetch(researchProject, submissionTuples, MessageReporter.UNUSED);
    }
}
