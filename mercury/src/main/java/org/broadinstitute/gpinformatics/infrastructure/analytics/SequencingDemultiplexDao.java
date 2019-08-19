package org.broadinstitute.gpinformatics.infrastructure.analytics;

import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.SequencingDemultiplexMetric;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.SequencingDemultiplexMetric_;
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
public class SequencingDemultiplexDao extends GenericDao {

    public List<SequencingDemultiplexMetric> findBySampleAlias(List<String> sampleAlias) {
        if (sampleAlias == null || sampleAlias.isEmpty()) {
            return Collections.emptyList();
        }

        List<SequencingDemultiplexMetric> resultList = new ArrayList<>();

        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<SequencingDemultiplexMetric> query = cb.createQuery(SequencingDemultiplexMetric.class);
        Root<SequencingDemultiplexMetric> root = query.from(SequencingDemultiplexMetric.class);

        Subquery<Date> sq = query.subquery(Date.class);
        Root<SequencingDemultiplexMetric> subRoot = sq.from(SequencingDemultiplexMetric.class);
        sq.select(cb.greatest(subRoot.get(SequencingDemultiplexMetric_.runDate)));
        sq.where(cb.equal(root.get(SequencingDemultiplexMetric_.sampleAlias), subRoot.get(SequencingDemultiplexMetric_.sampleAlias)));
        Predicate subQueryPredicate = cb.equal(root.get(SequencingDemultiplexMetric_.runDate), sq);

        Expression<String> parentExpression = root.get(SequencingDemultiplexMetric_.sampleAlias);
        Predicate parentPredicate = parentExpression.in(sampleAlias);

        query.where(cb.and(parentPredicate, subQueryPredicate));

        try {
            resultList.addAll(getEntityManager().createQuery(query).getResultList());
        } catch (NoResultException ignored) {
            return resultList;
        }

        return resultList;
    }

    public List<SequencingDemultiplexMetric> findByRunName(List<String> runNames) {
        if (runNames == null || runNames.isEmpty()) {
            return Collections.emptyList();
        }

        List<SequencingDemultiplexMetric> resultList = new ArrayList<>();

        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<SequencingDemultiplexMetric> query = cb.createQuery(SequencingDemultiplexMetric.class);
        Root<SequencingDemultiplexMetric> root = query.from(SequencingDemultiplexMetric.class);

        List<Predicate> predicates = new ArrayList<>();
        for (String run: runNames) {
            predicates.add(cb.equal(root.get(SequencingDemultiplexMetric_.runName), run));
        }

        query.where(cb.or(predicates.toArray(new Predicate[predicates.size()])));

        Subquery<Date> sq = query.subquery(Date.class);
        Root<SequencingDemultiplexMetric> subRoot = sq.from(SequencingDemultiplexMetric.class);
        sq.select(cb.greatest(root.get(SequencingDemultiplexMetric_.runDate)));
        sq.where(cb.equal(root.get(SequencingDemultiplexMetric_.runName), subRoot.get(SequencingDemultiplexMetric_.runName)));

        try {
            resultList.addAll(getEntityManager().createQuery(query).getResultList());
        } catch (NoResultException ignored) {
            return resultList;
        }

        return resultList;
    }

    public List<SequencingDemultiplexMetric> findByBarcodes(List<String> barcodes) {
        //TODO
        return null;
    }
}
