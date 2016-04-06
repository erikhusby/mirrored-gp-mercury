package org.broadinstitute.gpinformatics.infrastructure.metrics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlbeans.impl.xb.xsdschema.RestrictionDocument;
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
import org.hibernate.criterion.Restrictions;
import org.hornetq.utils.json.JSONArray;

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
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @PersistenceContext(unitName = "metrics_pu", type = PersistenceContextType.EXTENDED)
    private EntityManager entityManager;

    public List<Aggregation> fetch(List<String> projects, List<String> samples, List<Integer> versions) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Aggregation> criteriaQuery = criteriaBuilder.createQuery(Aggregation.class);

        Root<Aggregation> root = criteriaQuery.from(Aggregation.class);

        List<Predicate> predicateOfOrs = new ArrayList<>();
        Iterator<String> projectIterator = projects.iterator();
        Iterator<String> sampleIterator = samples.iterator();
        Iterator<Integer> versionIterator = versions.iterator();
        while (projectIterator.hasNext() && sampleIterator.hasNext() && versionIterator.hasNext()) {
            String project = projectIterator.next();
            String sample = sampleIterator.next();
            Integer version = versionIterator.next();
            List<Predicate> predicate = new ArrayList<>();

            predicate.add(criteriaBuilder.equal(root.get(Aggregation_.project), project));
            if (sample != null) {
                predicate.add(criteriaBuilder.equal(root.get(Aggregation_.sample), sample));
            }
            predicate.add(criteriaBuilder.equal(root.get(Aggregation_.version), version));

        /*
         * Look for the row where LIBRARY is NULL because otherwise there would be multiple results. Kathleen Tibbetts
         * said that this is the way to narrow it down to one.
         */

//            predicate.add(criteriaBuilder.isNull(root.get("library")));

            predicateOfOrs.add(criteriaBuilder.and(predicate.toArray(new Predicate[predicate.size()])));
        }

        criteriaQuery.where(criteriaBuilder.or(predicateOfOrs.toArray(new Predicate[predicateOfOrs.size()])),
                criteriaBuilder.isNull(root.get("library")));

        TypedQuery<Aggregation> query = entityManager.createQuery(criteriaQuery);
        List<Aggregation> aggregations = new ArrayList<>();
        try {
            aggregations = query.getResultList();
        } catch (NoResultException e) {
            log.info("Unable to retrieve aggregations based on given criteria");
        }
        fetchLod(aggregations);
//        for(Aggregation aggregation:aggregations) {
//            try {
//                TypedQuery<LevelOfDetection> lodQuery =
//                        entityManager.createNamedQuery(LevelOfDetection.LOD_QUERY_NAME, LevelOfDetection.class)
//                                .setParameter(SAMPLE_COLUMN, aggregation.getSample())
//                                .setParameter(PROJECT_COLUMN, aggregation.getProject())
//                                .setParameter(VERSION_COLUMN, aggregation.getVersion());
//                aggregation.setLevelOfDetection(lodQuery.getSingleResult());
//            } catch (NoResultException e) {
//                log.info(String.format("Unable to retrieve LOD info for Aggregation based on : " +
//                                       "Project %s, Sample %s, Version %d", aggregation.getProject(),
//                        aggregation.getSample(), aggregation.getVersion()));
//            }
//        }
        return aggregations;
    }
    /*
    public List<ProductOrder> findByWorkflow(@Nonnull Workflow workflow) {

            EntityManager entityManager = getEntityManager();
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

            CriteriaQuery<ProductOrder> criteriaQuery = criteriaBuilder.createQuery(ProductOrder.class);

            List<Predicate> predicates = new ArrayList<>();

            Root<ProductOrder> productOrderRoot = criteriaQuery.from(ProductOrder.class);
            Join<ProductOrder, Product> productJoin = productOrderRoot.join(ProductOrder_.product);

            predicates.add(criteriaBuilder.equal(productJoin.get(Product_.workflowName), workflow.getWorkflowName()));

            criteriaQuery.where(predicates.toArray(new Predicate[predicates.size()]));

            try {
                return entityManager.createQuery(criteriaQuery).getResultList();
            } catch (NoResultException ignored) {
                return null;
            }
        }
     */

    @SuppressWarnings("unchecked")
    public void fetchLod(List<Aggregation> aggregations) {
        if (aggregations.isEmpty()) {
            return;
        }
        CriteriaBuilder queryBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Tuple> tupleQuery = queryBuilder.createTupleQuery();
        Root<PicardFingerprint> root = tupleQuery.from(PicardFingerprint.class);
        Join<PicardFingerprint, PicardAnalysis> fingerPrintAnalysisJoin = root.join(PicardFingerprint_.picardAnalysis);
        Join<PicardAnalysis, AggregationReadGroup> readGroupPicardAnalysisJoin = fingerPrintAnalysisJoin.join(PicardAnalysis_.aggregationReadGroups);
        Join<AggregationReadGroup, Aggregation> aggregationAggregationReadGroupsJoin = readGroupPicardAnalysisJoin.join(AggregationReadGroup_.aggregation,
                JoinType.INNER);

        List<Predicate> predicates = new ArrayList<>();
        Set<Integer> aggregationIdList = new HashSet<>();
        for (Aggregation aggregation : aggregations) {
//            aggregationIdList.add(aggregation.getId());
            predicates.add(queryBuilder
                    .equal(aggregationAggregationReadGroupsJoin.get(Aggregation_.id), aggregation.getId()));
        }

        Path<Integer> aggregationPath = aggregationAggregationReadGroupsJoin.get(Aggregation_.id);
        Expression<Double> minExpression = queryBuilder.min(root.get(PicardFingerprint_.lodExpectedSample));
        Expression<Double> maxExpression = queryBuilder.max(root.get(PicardFingerprint_.lodExpectedSample));

//        aggregationAggregationReadGroupsJoin.equals(root.get(Aggregation_.id), aggregationIdList)

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
