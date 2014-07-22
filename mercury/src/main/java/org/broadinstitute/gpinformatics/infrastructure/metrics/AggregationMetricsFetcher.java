package org.broadinstitute.gpinformatics.infrastructure.metrics;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.AggregationReadGroup;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation_;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.PicardAnalysis;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.PicardAnalysis_;

import javax.ejb.Stateless;
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
 * <p/>
 * Not a {@link GenericDao} because it uses a different persistence unit.
 */
@Stateless
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
            Aggregation aggregation = query.getSingleResult();
            for (AggregationReadGroup aggregationReadGroup : aggregation.getAggregationReadGroups()) {
                setPicardAnalysis(aggregationReadGroup);
            }

            return aggregation;
        } catch (NoResultException e) {
            return null;
        }
    }

    private void setPicardAnalysis(AggregationReadGroup aggregationReadGroup){
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<PicardAnalysis> criteriaQuery = criteriaBuilder.createQuery(PicardAnalysis.class);
        Root<PicardAnalysis> root = criteriaQuery.from(PicardAnalysis.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder
                .equal(root.get(PicardAnalysis_.flowcellBarcode), aggregationReadGroup.getFlowcellBarcode()));
        predicates.add(criteriaBuilder.equal(root.get(PicardAnalysis_.lane), aggregationReadGroup.getLane()));
        predicates.add(criteriaBuilder.equal(root.get(PicardAnalysis_.flowcellBarcode), aggregationReadGroup.getFlowcellBarcode()));
        predicates.add(criteriaBuilder.equal(root.get(PicardAnalysis_.libraryName), aggregationReadGroup.getLibraryName()));

        criteriaQuery.where(predicates.toArray(new Predicate[predicates.size()]));

        TypedQuery<PicardAnalysis> query = entityManager.createQuery(criteriaQuery);
        List<PicardAnalysis> result = query.getResultList();
        aggregationReadGroup.setPicardAnalysis(result);
    }
}
