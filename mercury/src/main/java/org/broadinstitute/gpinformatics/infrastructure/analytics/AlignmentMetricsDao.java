package org.broadinstitute.gpinformatics.infrastructure.analytics;

import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.AlignmentMetric;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.AlignmentMetric_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@RequestScoped
@Stateful
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AlignmentMetricsDao extends GenericDao {

    public List<AlignmentMetric> findBySampleAlias(List<String> sampleAlias) {
        if (sampleAlias == null || sampleAlias.isEmpty()) {
            return Collections.emptyList();
        }

        List<AlignmentMetric> resultList = new ArrayList<>();

        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
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

        try {
            resultList.addAll(getEntityManager().createQuery(query).getResultList());
        } catch (NoResultException ignored) {
            return resultList;
        }

        return resultList;
    }
}
