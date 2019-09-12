package org.broadinstitute.gpinformatics.infrastructure.analytics;

import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.DemultiplexSampleMetric;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.SequencingDemultiplexMetric_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.CriteriaInClauseCreator;
import org.broadinstitute.gpinformatics.infrastructure.jpa.JPASplitter;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
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

@RequestScoped
@Stateful
@TransactionAttribute(TransactionAttributeType.NEVER)
public class SequencingDemultiplexDao {

    @PersistenceContext(type = PersistenceContextType.EXTENDED, unitName = "mercurydw_pu")
    private EntityManager entityManager;

    public List<DemultiplexSampleMetric> findBySampleAlias(List<String> sampleAlias) {
        if( sampleAlias == null || sampleAlias.isEmpty() ) {
            return Collections.emptyList();
        }
        return JPASplitter.runCriteriaQuery(sampleAlias, new CriteriaInClauseCreator<String>() {
            @Override
            public Query createCriteriaInQuery(Collection<String> parameterList) {
                CriteriaBuilder cb = entityManager.getCriteriaBuilder();
                CriteriaQuery<DemultiplexSampleMetric> query = cb.createQuery(DemultiplexSampleMetric.class);
                Root<DemultiplexSampleMetric> root = query.from(DemultiplexSampleMetric.class);

                Subquery<Date> sq = query.subquery(Date.class);
                Root<DemultiplexSampleMetric> subRoot = sq.from(DemultiplexSampleMetric.class);
                sq.select(cb.greatest(subRoot.get(SequencingDemultiplexMetric_.runDate)));
                sq.where(cb.equal(root.get(SequencingDemultiplexMetric_.sampleAlias), subRoot.get(SequencingDemultiplexMetric_.sampleAlias)));
                Predicate subQueryPredicate = cb.equal(root.get(SequencingDemultiplexMetric_.runDate), sq);

                Expression<String> parentExpression = root.get(SequencingDemultiplexMetric_.sampleAlias);
                Predicate parentPredicate = parentExpression.in(sampleAlias);

                query.where(cb.and(parentPredicate, subQueryPredicate));
                return entityManager.createQuery(query);
            }
        });
    }

    public List<DemultiplexSampleMetric> findByRunName(List<String> runNames) {
        if (runNames == null || runNames.isEmpty()) {
            return Collections.emptyList();
        }

        List<DemultiplexSampleMetric> resultList = new ArrayList<>();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<DemultiplexSampleMetric> query = cb.createQuery(DemultiplexSampleMetric.class);
        Root<DemultiplexSampleMetric> root = query.from(DemultiplexSampleMetric.class);

        List<Predicate> predicates = new ArrayList<>();
        for (String run: runNames) {
            predicates.add(cb.equal(root.get(SequencingDemultiplexMetric_.runName), run));
        }

        query.where(cb.or(predicates.toArray(new Predicate[predicates.size()])));

        Subquery<Date> sq = query.subquery(Date.class);
        Root<DemultiplexSampleMetric> subRoot = sq.from(DemultiplexSampleMetric.class);
        sq.select(cb.greatest(root.get(SequencingDemultiplexMetric_.runDate)));
        sq.where(cb.equal(root.get(SequencingDemultiplexMetric_.runName), subRoot.get(SequencingDemultiplexMetric_.runName)));

        try {
            resultList.addAll(entityManager.createQuery(query).getResultList());
        } catch (NoResultException ignored) {
            return resultList;
        }

        return resultList;
    }

    public List<DemultiplexSampleMetric> findByBarcodes(List<String> barcodes) {
        //TODO
        return null;
    }
}
