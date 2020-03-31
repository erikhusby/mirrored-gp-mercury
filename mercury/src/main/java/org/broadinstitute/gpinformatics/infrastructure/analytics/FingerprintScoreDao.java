package org.broadinstitute.gpinformatics.infrastructure.analytics;

import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.AlignmentMetric;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.AlignmentMetric_;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.FingerprintScore;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.FingerprintScore_;
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
import java.util.Set;
import java.util.stream.Collectors;

@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class FingerprintScoreDao {

    @PersistenceContext(type = PersistenceContextType.EXTENDED, unitName = "mercurydw_pu")
    private EntityManager entityManager;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void persist(FingerprintScore score) {
        entityManager.persist(score);
    }

    public List<FingerprintScore> findFingerprintScoresBySampleAlias(Set<String> sampleAlias) {
        if (sampleAlias == null || sampleAlias.isEmpty()) {
            return Collections.emptyList();
        }

        return JPASplitter.runCriteriaQuery(sampleAlias, new CriteriaInClauseCreator<String>() {
            @Override
            public Query createCriteriaInQuery(Collection<String> sampleAlias) {
                List<AlignmentMetric> resultList = new ArrayList<>();

                sampleAlias = sampleAlias.stream().map(sa -> sa + "_Aggregation").collect(Collectors.toList());

                CriteriaBuilder cb = entityManager.getCriteriaBuilder();
                CriteriaQuery<FingerprintScore> query = cb.createQuery(FingerprintScore.class);
                Root<FingerprintScore> root = query.from(FingerprintScore.class);

                // Build subquery to select the max run date per Sample Alias
                Subquery<Date> sq = query.subquery(Date.class);
                Root<FingerprintScore> subRoot = sq.from(FingerprintScore.class);
                sq.select(cb.greatest(subRoot.get(FingerprintScore_.runDate)));
                sq.where(cb.equal(root.get(FingerprintScore_.runName), subRoot.get(FingerprintScore_.runName)));
                Predicate subQueryPredicate = cb.equal(root.get(FingerprintScore_.runDate), sq);

                Expression<String> parentExpression = root.get(FingerprintScore_.runName);
                Predicate parentPredicate = parentExpression.in(sampleAlias);

                query.where(cb.and(parentPredicate, subQueryPredicate));

                return entityManager.createQuery(query);
            }
        });
    }
}
