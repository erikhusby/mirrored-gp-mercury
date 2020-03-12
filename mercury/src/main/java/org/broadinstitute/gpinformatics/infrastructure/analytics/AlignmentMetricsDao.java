package org.broadinstitute.gpinformatics.infrastructure.analytics;

import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.AlignmentMetric;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.AlignmentMetric_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.CriteriaInClauseCreator;
import org.broadinstitute.gpinformatics.infrastructure.jpa.JPASplitter;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequestScoped
@Stateful
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AlignmentMetricsDao {

    @PersistenceContext(type = PersistenceContextType.EXTENDED, unitName = "mercurydw_pu")
    private EntityManager entityManager;

    public List<AlignmentMetric> findBySampleAlias(Collection<String> sampleAlias) {
        if (sampleAlias == null || sampleAlias.isEmpty()) {
            return Collections.emptyList();
        }

        return JPASplitter.runCriteriaQuery(sampleAlias, new CriteriaInClauseCreator<String>() {
            @Override
            public Query createCriteriaInQuery(Collection<String> sampleAlias) {
                CriteriaBuilder cb = entityManager.getCriteriaBuilder();
                CriteriaQuery<AlignmentMetric> query = cb.createQuery(AlignmentMetric.class);
                Root<AlignmentMetric> root = query.from(AlignmentMetric.class);

                // Build subquery to select the max run date per Sample Alias
                Subquery<Date> sq = query.subquery(Date.class);
                Root<AlignmentMetric> subRoot = sq.from(AlignmentMetric.class);
                sq.select(cb.greatest(subRoot.get(AlignmentMetric_.runDate)));
                sq.where(cb.equal(root.get(AlignmentMetric_.sampleAlias), subRoot.get(AlignmentMetric_.sampleAlias)));
                Predicate subQueryPredicate = cb.equal(root.get(AlignmentMetric_.runDate), sq);

                Expression<String> parentExpression = root.get(AlignmentMetric_.sampleAlias);
                Predicate parentPredicate = parentExpression.in(sampleAlias);

                query.where(cb.and(parentPredicate, subQueryPredicate));

                return entityManager.createQuery(query);
            }
        });
    }

    public List<AlignmentMetric> findAggregationBySampleAlias(Collection<String> sampleAlias) {
        if (sampleAlias == null || sampleAlias.isEmpty()) {
            return Collections.emptyList();
        }

        return JPASplitter.runCriteriaQuery(sampleAlias, new CriteriaInClauseCreator<String>() {
            @Override
            public Query createCriteriaInQuery(Collection<String> sampleAlias) {
                List<AlignmentMetric> resultList = new ArrayList<>();

                sampleAlias = sampleAlias.stream().map(sa -> sa + "_Aggregation").collect(Collectors.toList());

                CriteriaBuilder cb = entityManager.getCriteriaBuilder();
                CriteriaQuery<AlignmentMetric> query = cb.createQuery(AlignmentMetric.class);
                Root<AlignmentMetric> root = query.from(AlignmentMetric.class);

                // Build subquery to select the max run date per Sample Alias
                Subquery<Date> sq = query.subquery(Date.class);
                Root<AlignmentMetric> subRoot = sq.from(AlignmentMetric.class);
                sq.select(cb.greatest(subRoot.get(AlignmentMetric_.runDate)));
                sq.where(cb.equal(root.get(AlignmentMetric_.readGroup), subRoot.get(AlignmentMetric_.readGroup)));
                Predicate subQueryPredicate = cb.equal(root.get(AlignmentMetric_.runDate), sq);

                Expression<String> parentExpression = root.get(AlignmentMetric_.readGroup);
                Predicate parentPredicate = parentExpression.in(sampleAlias);

                query.where(cb.and(parentPredicate, subQueryPredicate));

                return entityManager.createQuery(query);
            }
        });
    }

    public Map<String, AlignmentMetric> findMapBySampleAlias(Collection<String> sampleAlias) {
        return findAggregationBySampleAlias(sampleAlias).stream().collect(Collectors.toMap(AlignmentMetric::getSampleAlias,
                Function.identity()));
    }
}
