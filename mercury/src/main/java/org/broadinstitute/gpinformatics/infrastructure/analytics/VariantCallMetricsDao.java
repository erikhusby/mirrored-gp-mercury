package org.broadinstitute.gpinformatics.infrastructure.analytics;

import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.VariantCallMetric;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.VariantCallMetric_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.CriteriaInClauseCreator;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@RequestScoped
@Stateful
@TransactionAttribute(TransactionAttributeType.NEVER)
public class VariantCallMetricsDao extends GenericDao {

    @PersistenceContext(type = PersistenceContextType.EXTENDED, unitName = "mercurydw_pu")
    private EntityManager entityManager;

    public List<VariantCallMetric> findBySampleAlias(List<String> sampleAlias) {
        if( sampleAlias == null || sampleAlias.isEmpty() ) {
            return Collections.emptyList();
        }
        return JPASplitter.runCriteriaQuery(sampleAlias, new CriteriaInClauseCreator<String>() {
            @Override
            public Query createCriteriaInQuery(Collection<String> parameterList) {
                CriteriaBuilder cb = entityManager.getCriteriaBuilder();
                CriteriaQuery<VariantCallMetric> query = cb.createQuery(VariantCallMetric.class);
                Root<VariantCallMetric> root = query.from(VariantCallMetric.class);

                Subquery<Date> sq = query.subquery(Date.class);
                Root<VariantCallMetric> subRoot = sq.from(VariantCallMetric.class);
                sq.select(cb.greatest(subRoot.get(VariantCallMetric_.runDate)));
                sq.where(cb.equal(root.get(VariantCallMetric_.sampleAlias), subRoot.get(VariantCallMetric_.sampleAlias)));
                Predicate subQueryPredicate = cb.equal(root.get(VariantCallMetric_.runDate), sq);

                Expression<String> parentExpression = root.get(VariantCallMetric_.sampleAlias);
                Predicate parentPredicate = parentExpression.in(sampleAlias);

                query.where(cb.and(parentPredicate, subQueryPredicate));
                return entityManager.createQuery(query);
            }
        });
    }
}
