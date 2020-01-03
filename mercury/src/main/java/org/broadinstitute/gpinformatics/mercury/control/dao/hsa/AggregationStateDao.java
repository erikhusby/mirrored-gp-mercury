package org.broadinstitute.gpinformatics.mercury.control.dao.hsa;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AggregationState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AggregationState_;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber_;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample_;

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
import javax.persistence.criteria.SetJoin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class AggregationStateDao extends GenericDao {

    public List<AggregationState> findBySampleKey(String sampleKey) {
        if (sampleKey == null) {
            return Collections.emptyList();
        }

        List<AggregationState> resultList = new ArrayList<>();

        Set<String> sampleAlias = Collections.singleton(sampleKey);

        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<AggregationState> query = cb.createQuery(AggregationState.class);
        Root<AggregationState> root = query.from(AggregationState.class);

        final SetJoin<AggregationState, MercurySample> setJoin =
                root.join(AggregationState_.mercurySamples);

        Expression<String> parentExpression = setJoin.get(MercurySample_.sampleKey);
        Predicate parentPredicate = parentExpression.in(sampleAlias);
        query.where(parentPredicate);
        try {
            resultList.addAll(getEntityManager().createQuery(query).getResultList());
        } catch (NoResultException ignored) {
            return resultList;
        }

        return resultList.stream()
                .sorted(new State.StateStartComparator().reversed())
                .collect(Collectors.toList());
    }

    public List<AggregationState> findBySample(MercurySample mercurySample) {
        return findBySampleKey(mercurySample.getSampleKey());
    }

    public AggregationState findBySampleWithChambers(MercurySample mercurySample,
                                                           Set<IlluminaSequencingRunChamber> chambers) {
        for (AggregationState state: findBySampleKey(mercurySample.getSampleKey())) {
            if (state.getSequencingRunChambers().equals(chambers)) {
                return state;
            }
        }
        return null;
    }
}
